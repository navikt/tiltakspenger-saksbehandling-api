package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

/**
 * All logging for oppslag mot ao-oppfolgingskontor skjer her, med loggkontekst (sakId/saksnummer/...) i loggmeldingene for sporbarhet.
 * Navkontor er stedslokaliserende persondata, så rådata logges kun til sikkerlogg.
 */
class NavkontorService(
    private val oppfølgingskontorKlient: OppfølgingskontorKlient,
    private val logger: KLogger = KotlinLogging.logger(NavkontorService::class.java.name),
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    /**
     * Henter kontortilhørigheten og kontorhistorikken parallelt og sammenligner kontorId.
     * Er kontorId lik, brukes kontortilhørigheten.
     * Er kontorId ulik, brukes [Kontorhistorikk.nyesteAktuelleKontor].
     * Har bare én av kildene et kontor (den andre feilet eller fant ikke noe kontor), brukes den.
     * Avvik mellom to vellykkede kall logges som error.
     *
     * Kaster [IllegalStateException] dersom ingen av kildene har et kontor.
     */
    suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Navkontor {
        val (tilhørighetResultat, historikkResultat) = coroutineScope {
            val tilhørighet = async { oppfølgingskontorKlient.hentKontorTilhørighet(fnr) }
            val historikk = async { oppfølgingskontorKlient.hentKontorhistorikk(fnr) }
            tilhørighet.await() to historikk.await()
        }
        tilhørighetResultat.onLeft { it.logg(OPERASJON_KONTORTILHØRIGHET, loggkontekst) }
        historikkResultat.onLeft { it.logg(OPERASJON_KONTORHISTORIKK, loggkontekst) }

        val fraTilhørighet = tilhørighetResultat.getOrNull()?.kontorTilhørighet
        val fraHistorikk = historikkResultat.getOrNull()?.kontorhistorikk?.nyesteAktuelleKontor()
        val avvik = fraTilhørighet?.kontorId != fraHistorikk?.kontorId

        val navkontor = when {
            fraHistorikk != null && avvik -> fraHistorikk.tilNavkontor()

            fraTilhørighet != null -> fraTilhørighet.tilNavkontor()

            else -> {
                val beskrivelse = "kontortilhørighet: ${tilhørighetResultat.beskrivelse()}, kontorhistorikk: ${historikkResultat.beskrivelse()}"
                val tilhørighetMetadata = tilhørighetResultat.fold({ it.httpKlientMetadata }, { it.httpKlientMetadata })
                val historikkMetadata = historikkResultat.fold({ it.httpKlientMetadata }, { it.httpKlientMetadata })
                logger.error { "Navkontor: fant ikke kontor i noen av kildene ($beskrivelse). ${sikkerlogg.seSikkerlogg} $loggkontekst" }
                sikkerlogg.error {
                    "Navkontor: fant ikke kontor i noen av kildene ($beskrivelse). " +
                        "Kontortilhørighet: ${tilhørighetMetadata.rådata()}. Kontorhistorikk: ${historikkMetadata.rådata()}. $loggkontekst"
                }
                // Kun nøytrale beskrivelser i meldingen - exception-meldinger havner i vanlig logg hos konsumentene.
                error("Kunne ikke hente navkontor: $beskrivelse")
            }
        }

        // Feilede kall er allerede logget; her logger vi bare avvik mellom to vellykkede kall.
        if (avvik && tilhørighetResultat.isRight() && historikkResultat.isRight()) {
            val kilde = if (fraHistorikk != null) "kontorhistorikk" else "kontortilhørighet"
            logger.error { "Navkontor: ulikt svar fra kontortilhørighet og kontorhistorikk, bruker $kilde. ${sikkerlogg.seSikkerlogg} $loggkontekst" }
            sikkerlogg.error {
                "Navkontor: ulikt svar fra kontortilhørighet og kontorhistorikk, bruker $kilde. " +
                    "Kontortilhørighet: ${fraTilhørighet?.let { "kontorId=${it.kontorId} (type=${it.kontorType})" } ?: "ingen kontor"}. " +
                    "Kontorhistorikk: ${fraHistorikk?.let { "kontorId=${it.kontorId} (type=${it.kontorType})" } ?: "ingen kontor"}. $loggkontekst"
            }
        }
        return navkontor
    }

    /**
     * Henter hele kontorhistorikken til personen.
     * Kaster [IllegalStateException] dersom kallet feilet.
     */
    suspend fun hentKontorhistorikk(
        fnr: Fnr,
        loggkontekst: String,
    ): Kontorhistorikk {
        return oppfølgingskontorKlient.hentKontorhistorikk(fnr).getOrElse { feil ->
            feil.logg(OPERASJON_KONTORHISTORIKK, loggkontekst)
            // Kun beskrivelse() i meldingen - feilens toString() bærer rå request/respons med persondata, og exception-meldinger havner i vanlig logg hos konsumentene.
            error("Kunne ikke hente kontorhistorikk: ${feil.beskrivelse()}")
        }.kontorhistorikk
    }

    /** Én logghendelse per feilsituasjon: vanlig logg uten rådata + sikkerlogg med rå request/respons. */
    private fun KanIkkeHenteOppfølgingskontor.logg(
        operasjon: String,
        loggkontekst: String,
    ) {
        when (this) {
            is KanIkkeHenteOppfølgingskontor.KallFeilet -> httpKlientError.loggFeil(logger, operasjon, loggkontekst, sikkerlogg)

            is KanIkkeHenteOppfølgingskontor.UventetHttpStatus -> httpKlientError.loggFeil(logger, operasjon, loggkontekst, sikkerlogg)

            is KanIkkeHenteOppfølgingskontor.GraphQlFeil -> {
                logger.error { "Feil ved $operasjon. $loggkontekst. Tjenesten svarte med GraphQL-feil. ${sikkerlogg.seSikkerlogg}" }
                sikkerlogg.error {
                    "Feil ved $operasjon. $loggkontekst. Tjenesten svarte med GraphQL-feil. " +
                        "request=${httpKlientMetadata.rawRequestString}, response=${httpKlientMetadata.rawResponseString}."
                }
            }
        }
    }

    private fun Either<KanIkkeHenteOppfølgingskontor, *>.beskrivelse(): String =
        fold(ifLeft = { it.beskrivelse() }, ifRight = { "ingen kontor" })

    private fun HttpKlientMetadata.rådata(): String = "request=$rawRequestString, response=$rawResponseString"

    private companion object {
        const val OPERASJON_KONTORTILHØRIGHET = "henting av kontortilhørighet"
        const val OPERASJON_KONTORHISTORIKK = "henting av kontorhistorikk"
    }
}
