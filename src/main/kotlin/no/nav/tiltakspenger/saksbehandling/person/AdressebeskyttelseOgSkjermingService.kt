package no.nav.tiltakspenger.saksbehandling.person

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrThrow
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.felles.loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.sikkerloggkontekst
import java.time.Duration

/**
 * Slår opp adressebeskyttelsen og skjermingen til personer i PDL og skjermingsregisteret.
 * Fra PDL hentes bare adressebeskyttelsen, ingen andre personopplysninger.
 *
 * Svaret per person caches en kort stund, så de samme personene ikke slås opp på nytt ved hvert kall.
 * En endring i PDL eller skjermingsregisteret slår derfor først inn når oppføringen har gått ut av cachen.
 * Tjenesten gjør ingen tilgangskontroll og logger ikke; feilene bærer sin egen loggkontekst og logges av route-laget.
 */
class AdressebeskyttelseOgSkjermingService(
    private val personKlient: PersonKlient,
    private val skjermingsklient: FellesSkjermingsklient,
) {
    private val cache: Cache<Fnr, AdressebeskyttelseOgSkjerming> = Caffeine.newBuilder()
        .expireAfterWrite(LEVETID)
        .maximumSize(MAKS_ANTALL_PERSONER)
        .build()

    /**
     * Svarer med adressebeskyttelsen og skjermingen til hver av [fnrs].
     * PDL og skjermingsregisteret spørres bare om personene som ikke ligger i cachen, og bolkene slås opp samtidig.
     * En person PDL ikke gir avklart adressebeskyttelse for, regnes som ugradert, og en person skjermingsregisteret ikke svarer for, som ikke skjermet.
     */
    suspend fun hent(
        fnrs: List<Fnr>,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteAdressebeskyttelseEllerSkjerming, Map<Fnr, AdressebeskyttelseOgSkjerming>> = either {
        val unike = fnrs.distinct()
        val fraCache = cache.getAllPresent(unike)
        val nye = coroutineScope {
            unike.filterNot { it in fraCache }
                .chunked(MAKS_ANTALL_PER_KALL)
                .map { bolk -> async { slåOpp(bolk, correlationId) } }
                .awaitAll()
        }.fold(emptyMap<Fnr, AdressebeskyttelseOgSkjerming>()) { alle, bolk -> alle + bolk.bind() }
        cache.putAll(nye)
        fraCache + nye
    }

    /**
     * Skjermingsregisteret og PDL spørres samtidig.
     * Bolkene er aldri tomme, så toNonEmptyListOrThrow kaster ikke.
     */
    private suspend fun slåOpp(
        fnrs: List<Fnr>,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteAdressebeskyttelseEllerSkjerming, Map<Fnr, AdressebeskyttelseOgSkjerming>> = either {
        val (skjermingssvar, adressebeskyttelsessvar) = coroutineScope {
            val skjerming = async { skjermingsklient.erSkjermetPersoner(fnrs.toNonEmptyListOrThrow(), correlationId) }
            val adressebeskyttelse = async { personKlient.hentAdressebeskyttelse(fnrs) }
            skjerming.await() to adressebeskyttelse.await()
        }
        val skjermet = skjermingssvar
            .mapLeft {
                KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotSkjerming(
                    loggkontekst = it.httpKlientError.loggkontekst("skjermingsoppslag for ${fnrs.size} personer"),
                    sikkerloggkontekst = it.httpKlientError.sikkerloggkontekst("skjermingsoppslag"),
                )
            }.bind()
        val adressebeskyttelse = adressebeskyttelsessvar.bind()
        fnrs.associateWith { fnr ->
            AdressebeskyttelseOgSkjerming(
                adressebeskyttelse = adressebeskyttelse[fnr] ?: Adressebeskyttelse.UGRADERT,
                skjermet = skjermet[fnr] == true,
            )
        }
    }

    private companion object {
        val LEVETID: Duration = Duration.ofMinutes(10)
        const val MAKS_ANTALL_PERSONER = 50_000L

        /** PDL tar maks 1000 identer i et bolkoppslag. */
        const val MAKS_ANTALL_PER_KALL = 1000
    }
}

/** Adressebeskyttelsen og skjermingen til én person. */
data class AdressebeskyttelseOgSkjerming(
    val adressebeskyttelse: Adressebeskyttelse,
    val skjermet: Boolean,
) {
    val harAdressebeskyttelseEllerSkjerming: Boolean get() = adressebeskyttelse != Adressebeskyttelse.UGRADERT || skjermet
}
