package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arrangør
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakelsesomfang
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.EksternDeltakelseId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse

/**
 * Deltakelsen slik tiltakshistorikk ville levert den, til fakes som trenger libs-domenet.
 * Rundturen tilbake via `tilTiltaksdeltakelseSomFratarRett` gir samme verdi, så lenge statusen finnes hos kilden.
 * Tittel, arrangør og gjennomføring finnes ikke i den interne modellen, og blir tomme.
 */
fun TiltaksdeltakelseSomFratarRett.tilLibsDeltakelse(): Tiltaksdeltakelse.FratarRett {
    val fraOgMed = deltakelseFraOgMed
    val tilOgMed = deltakelseTilOgMed
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
        periode = if (fraOgMed != null && tilOgMed != null) Periode(fraOgMed, tilOgMed) else ÅpenPeriode(fraOgMed, tilOgMed),
    )
}
