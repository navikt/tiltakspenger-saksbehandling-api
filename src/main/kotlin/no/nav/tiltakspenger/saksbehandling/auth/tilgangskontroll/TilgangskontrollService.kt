package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import java.util.concurrent.ConcurrentHashMap

/**
 * [log] og [sikkerlogg] er parametere uten default-verdi, så kallstedet må velge hvor linjene havner.
 * Da kan en test også sjekke at ingenting skrives til noen av dem når alt går bra.
 * [no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext] sender inn loggeren med klassens navn, så navnet i Loki er uendret.
 */
class TilgangskontrollService(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val sakService: SakService,
    private val log: KLogger,
    private val sikkerlogg: Sikkerlogg,
) {

    suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
    ) {
        val vurdering = tilgangsmaskinClient.harTilgangTilPerson(fnr, saksbehandlerToken).getOrElse { feil ->
            håndterTilgangskontrollFeil(feil, saksbehandler, "Saksbehandler ${saksbehandler.navIdent}")
        }

        when (vurdering) {
            is Tilgangsvurdering.Avvist -> {
                if (vurdering.årsak == TilgangsvurderingAvvistÅrsak.UKJENT) {
                    varsleOmUkjenteAvvisningskoder(
                        setOf(vurdering.metadata.avvisningskode),
                        "Saksbehandler ${saksbehandler.navIdent}",
                    )
                }
                throw TilgangException(
                    vurdering.årsak.toTilgangsnektårsak(),
                    "Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person: ${vurdering.begrunnelse}",
                )
            }

            Tilgangsvurdering.Godkjent -> Unit
        }
    }

    suspend fun harTilgangTilPersonForSakId(
        sakId: SakId,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        try {
            val fnr = sakService.hentFnrForSakId(sakId)
            harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
        } catch (tilgangException: TilgangException) {
            throw tilgangException
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person for sakId $sakId: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
        }
    }

    suspend fun harTilgangTilPersonerForSakIder(
        sakIder: NonEmptySet<SakId>,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        sakIder.forEach {
            try {
                val fnr = sakService.hentFnrForSakId(it)
                harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
            } catch (tilgangException: TilgangException) {
                throw tilgangException
            } catch (e: Exception) {
                log.error { "Noe gikk galt ved sjekk av tilgang for person for sakId $it: ${e.message}" }
                throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
            }
        }
    }

    suspend fun harTilgangTilPersonForSaksnummer(
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        try {
            val fnr = sakService.hentFnrForSaksnummer(saksnummer)
            harTilgangTilPerson(fnr, saksbehandlerToken, saksbehandler)
        } catch (tilgangException: TilgangException) {
            throw tilgangException
        } catch (e: Exception) {
            log.error { "Noe gikk galt ved sjekk av tilgang for person for saksnummer $saksnummer: ${e.message}" }
            throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent}: ${e.message}}")
        }
    }

    /**
     * Et vellykket bulkoppslag logges ikke.
     * Benken henter tilganger ved hver sidevisning, og ruten logger allerede forespørselen, så hendelsen er synlig uten at fødselsnumrene på siden skrives til sikkerlogg hver gang.
     * Avvik logges: en feil får sin linje i [loggTilgangskontrollFeil], og en avvisningskode vi ikke kjenner, får sin egen warn-linje og teller i [varsleOmUkjenteAvvisningskoder].
     */
    suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<TilgangskontrollFeil, Map<Fnr, TilgangsvurderingBulk>> {
        val kontekst = "Saksbehandler ${saksbehandler.navIdent}, ${fnrs.size} personer, correlationId $correlationId"
        return tilgangsmaskinClient.harTilgangTilPersoner(fnrs, saksbehandlerToken)
            .onLeft { loggTilgangskontrollFeil(it, kontekst) }
            .map { respons ->
                varsleOmUkjenteAvvisningskoder(respons.body.ukjenteAvvisningskoder, kontekst)
                respons.body.perFnr
            }
    }

    /**
     * Kodene denne instansen allerede har logget.
     * Settet nullstilles ved oppstart, og det er riktig: etter en deploy er koden enten lagt inn, eller så skal vi minnes på den igjen.
     */
    private val alleredeLoggedeAvvisningskoder: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * En avvisningskode vi ikke kjenner, avviser fortsatt tilgangen, men raden får ingen markør.
     * Koden er Tilgangsmaskinens regelnavn og ikke en personopplysning, så den hører hjemme i vanlig logg.
     *
     * Telleren økes hver gang, for det er den alarmen i `.nais/alerts.yml` står på.
     * Logglinja skrives bare første gang koden dukker opp i denne poden.
     * En ukjent kode treffer hver rad den gjelder, hver gang saksbehandleren åpner benken, så uten den sperren ville hver sidevisning gitt en ny warn-linje.
     */
    private fun varsleOmUkjenteAvvisningskoder(
        ukjenteAvvisningskoder: Set<String>,
        kontekst: String,
    ) {
        ukjenteAvvisningskoder.forEach { kode ->
            MetricRegister.TILGANGSMASKIN_UKJENT_AVVISNINGSKODE.labelValues(kode).inc()

            if (alleredeLoggedeAvvisningskoder.add(kode)) {
                log.warn { "Ukjent avvisningskode fra Tilgangsmaskinen: $kode. $kontekst." }
            }
        }
    }

    /**
     * Logger feilen én gang, med samme deling mellom vanlig logg og sikkerlogg som httpklient sin egen [loggFeil].
     * Beskrivelsen i [TilgangskontrollFeil.UgyldigSvar] er vår egen og PII-fri, mens den rå responsen kun hører hjemme i sikkerlogg.
     */
    private fun loggTilgangskontrollFeil(
        feil: TilgangskontrollFeil,
        kontekst: String,
    ) {
        when (feil) {
            is TilgangskontrollFeil.Uventet -> feil.underliggende.loggFeil(
                logger = log,
                operasjon = "tilgangskontroll mot tilgangsmaskinen",
                kontekst = kontekst,
                sikkerlogg = sikkerlogg,
            )

            TilgangskontrollFeil.ForMangeIdenter -> log.error {
                "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. Ba om tilgang for flere enn maksgrensen."
            }

            is TilgangskontrollFeil.UgyldigSvar -> {
                log.error { "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. ${feil.beskrivelse} Endepunkt: ${feil.metadata.endepunkt}. Se sikkerlogg for responsen." }
                sikkerlogg.error { "Feil ved tilgangskontroll mot tilgangsmaskinen. $kontekst. ${feil.beskrivelse} Endepunkt: ${feil.metadata.endepunkt}. Response: ${feil.metadata.rawResponseString}." }
            }
        }
    }

    /**
     * Enkeltoppslagets stier signaliserer fortsatt med kast; loggingen er felles med bulkstien.
     */
    private fun håndterTilgangskontrollFeil(
        feil: TilgangskontrollFeil,
        saksbehandler: Saksbehandler,
        kontekst: String,
    ): Nothing {
        loggTilgangskontrollFeil(feil, kontekst)
        throw RuntimeException("Klarte ikke gjøre tilgangskontroll for saksbehandler ${saksbehandler.navIdent} - feil mot tilgangsmaskinen")
    }
}
