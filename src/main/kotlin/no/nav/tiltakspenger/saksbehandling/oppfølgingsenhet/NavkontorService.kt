package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr

class NavkontorService(
    private val navkontorKlient: NavkontorKlient,
) {
    /**
     * Returnerer [Navkontor] for bakoverkompatibilitet. Klienten returnerer rik metadata
     * ([NavkontorMedMetadata]) som vi senere vil ønske å lagre - se [hentNavkontorMedMetadata].
     * Kaster [IllegalStateException] dersom klienten ikke klarte å hente navkontor.
     *
     * Logger ikke selv - all logging for navkontor-oppslag skjer i sammenligningsklienten, som får
     * [loggkontekst] (sakId/saksnummer/...) med i loggmeldingene for sporbarhet.
     */
    suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Navkontor {
        return hentNavkontorMedMetadata(
            fnr = fnr,
            loggkontekst = loggkontekst,
        ).fold(
            ifLeft = { feil ->
                // Kun beskrivelse() i meldingen - feilens toString() bærer rå request/respons med persondata,
                // og exception-meldinger havner i vanlig logg hos konsumentene.
                error("Kunne ikke hente navkontor: ${feil.beskrivelse()}")
            },
            ifRight = { it.navkontor },
        )
    }

    suspend fun hentNavkontorMedMetadata(
        fnr: Fnr,
        loggkontekst: String,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> {
        return navkontorKlient.hentNavkontor(
            fnr = fnr,
            loggkontekst = loggkontekst,
        )
    }
}
