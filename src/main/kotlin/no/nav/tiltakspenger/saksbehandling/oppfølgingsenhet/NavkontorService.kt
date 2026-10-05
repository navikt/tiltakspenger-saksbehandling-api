package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

class NavkontorService(
    private val kontorhistorikkKlient: KontorhistorikkKlient,
    private val logger: KLogger = KotlinLogging.logger(NavkontorService::class.java.name),
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    /**
     * Henter kontorhistorikken og returnerer [Kontorhistorikk.nyesteAktuelleKontor] som [Navkontor].
     * Kaster [IllegalStateException] dersom kallet feilet eller historikken ikke har et aktuelt kontor.
     *
     * All logging for navkontor-oppslaget skjer her, med [loggkontekst] (sakId/saksnummer/...) i loggmeldingene for sporbarhet.
     * Navkontor er stedslokaliserende persondata, så rådata logges kun til sikkerlogg.
     */
    suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Navkontor {
        val resultat = kontorhistorikkKlient.hentKontorhistorikk(fnr).getOrElse { feil ->
            feil.logg(loggkontekst)
            // Kun beskrivelse() i meldingen - feilens toString() bærer rå request/respons med persondata, og exception-meldinger havner i vanlig logg hos konsumentene.
            error("Kunne ikke hente navkontor: ${feil.beskrivelse()}")
        }
        val kontor = resultat.kontorhistorikk.nyesteAktuelleKontor()
        if (kontor == null) {
            logger.error { "Navkontor: kontorhistorikken har ikke et aktuelt kontor. ${sikkerlogg.seSikkerlogg} $loggkontekst" }
            sikkerlogg.error {
                "Navkontor: kontorhistorikken har ikke et aktuelt kontor. " +
                    "request=${resultat.httpKlientMetadata.rawRequestString}, response=${resultat.httpKlientMetadata.rawResponseString}. $loggkontekst"
            }
            error("Kunne ikke hente navkontor: kontorhistorikken har ikke et aktuelt kontor")
        }
        return kontor.tilNavkontor()
    }

    /** Én logghendelse per feilsituasjon: vanlig logg uten rådata + sikkerlogg med rå request/respons. */
    private fun KanIkkeHenteKontorhistorikk.logg(loggkontekst: String) {
        when (this) {
            is KanIkkeHenteKontorhistorikk.KallFeilet -> httpKlientError.loggFeil(logger, OPERASJON, loggkontekst, sikkerlogg)

            is KanIkkeHenteKontorhistorikk.UventetHttpStatus -> httpKlientError.loggFeil(logger, OPERASJON, loggkontekst, sikkerlogg)

            is KanIkkeHenteKontorhistorikk.GraphQlFeil -> {
                logger.error { "Feil ved $OPERASJON. $loggkontekst. Tjenesten svarte med GraphQL-feil. ${sikkerlogg.seSikkerlogg}" }
                sikkerlogg.error {
                    "Feil ved $OPERASJON. $loggkontekst. Tjenesten svarte med GraphQL-feil. " +
                        "request=${httpKlientMetadata.rawRequestString}, response=${httpKlientMetadata.rawResponseString}."
                }
            }
        }
    }

    private companion object {
        const val OPERASJON = "henting av kontorhistorikk (navkontor)"
    }
}
