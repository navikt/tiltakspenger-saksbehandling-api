package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

class NavkontorService(
    private val kontorTilhørighetKlient: KontorTilhørighetKlient,
    private val logger: KLogger = KotlinLogging.logger(NavkontorService::class.java.name),
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    /**
     * Henter kontortilhørigheten og returnerer den som [Navkontor].
     * Kaster [IllegalStateException] dersom kallet feilet eller personen ikke har noen kontortilhørighet.
     *
     * All logging for navkontor-oppslaget skjer her, med [loggkontekst] (sakId/saksnummer/...) i loggmeldingene for sporbarhet.
     * Navkontor er stedslokaliserende persondata, så rådata logges kun til sikkerlogg.
     */
    suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Navkontor {
        val resultat = kontorTilhørighetKlient.hentKontorTilhørighet(fnr).getOrElse { feil ->
            feil.logg(loggkontekst)
            // Kun beskrivelse() i meldingen - feilens toString() bærer rå request/respons med persondata, og exception-meldinger havner i vanlig logg hos konsumentene.
            error("Kunne ikke hente navkontor: ${feil.beskrivelse()}")
        }
        val kontor = resultat.kontorTilhørighet
        if (kontor == null) {
            logger.error { "Navkontor: personen har ingen kontortilhørighet. ${sikkerlogg.seSikkerlogg} $loggkontekst" }
            sikkerlogg.error {
                "Navkontor: personen har ingen kontortilhørighet. " +
                    "request=${resultat.httpKlientMetadata.rawRequestString}, response=${resultat.httpKlientMetadata.rawResponseString}. $loggkontekst"
            }
            error("Kunne ikke hente navkontor: personen har ingen kontortilhørighet")
        }
        return kontor.tilNavkontor()
    }

    /** Én logghendelse per feilsituasjon: vanlig logg uten rådata + sikkerlogg med rå request/respons. */
    private fun KanIkkeHenteKontorTilhørighet.logg(loggkontekst: String) {
        when (this) {
            is KanIkkeHenteKontorTilhørighet.KallFeilet -> httpKlientError.loggFeil(logger, OPERASJON, loggkontekst, sikkerlogg)

            is KanIkkeHenteKontorTilhørighet.UventetHttpStatus -> httpKlientError.loggFeil(logger, OPERASJON, loggkontekst, sikkerlogg)

            is KanIkkeHenteKontorTilhørighet.GraphQlFeil -> {
                logger.error { "Feil ved $OPERASJON. $loggkontekst. Tjenesten svarte med GraphQL-feil. ${sikkerlogg.seSikkerlogg}" }
                sikkerlogg.error {
                    "Feil ved $OPERASJON. $loggkontekst. Tjenesten svarte med GraphQL-feil. " +
                        "request=${httpKlientMetadata.rawRequestString}, response=${httpKlientMetadata.rawResponseString}."
                }
            }
        }
    }

    private companion object {
        const val OPERASJON = "henting av kontortilhørighet (navkontor)"
    }
}
