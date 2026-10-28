package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arrangør
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakelsesomfang
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.EksternDeltakelseId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import java.time.LocalDate

/**
 * Deltakelsen slik tiltakshistorikk ville levert den, til fakes som trenger libs-domenet.
 * Rundturen tilbake via `tilTiltaksdeltakelseSomFratarRett` gir samme verdi, så lenge statusen finnes hos kilden.
 * Tittel, arrangør og gjennomføring finnes ikke i den interne modellen, og blir tomme.
 */
fun TiltaksdeltakelseSomFratarRett.tilLibsDeltakelse(): Tiltaksdeltakelse.FratarRett {
    return Tiltaksdeltakelse.FratarRett(
        id = EksternDeltakelseId(eksternDeltakelseId),
        kildestatus = tilKildestatus(kilde, deltakelseStatus),
        tiltakstypenavn = typeNavn,
        tiltakskodeFraKilden = tiltakskodeFraKilden,
        tittel = null,
        arrangør = Arrangør(hovedenhet = null, underenhet = null),
        omfang = Deltakelsesomfang(
            deltakelsesprosent = deltakelseProsent,
            dagerPerUke = antallDagerPerUke,
            deltidsprosentPåGjennomføring = null,
        ),
        gjennomføringId = null,
        periode = periode,
    )
}

/**
 * Kopi med ny start- og/eller sluttdato.
 * Datoene som ikke sendes inn beholdes.
 */
fun TiltaksdeltakelseSomFratarRett.medPeriode(
    fraOgMed: LocalDate? = periode.fraOgMed,
    tilOgMed: LocalDate? = periode.tilOgMed,
): TiltaksdeltakelseSomFratarRett = copy(periode = lukketEllerÅpenPeriode(fraOgMed, tilOgMed))
