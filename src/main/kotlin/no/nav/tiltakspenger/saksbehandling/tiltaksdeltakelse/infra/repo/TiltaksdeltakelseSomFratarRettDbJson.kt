package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import java.time.LocalDate

/**
 * Lagres som en del av saksopplysningene.
 *
 * @param tittel Mangler i saksopplysninger lagret før tittelen ble innført.
 */
data class TiltaksdeltakelseSomFratarRettDbJson(
    val eksternDeltakelseId: String,
    val typeNavn: String,
    val tittel: String?,
    val tiltakskodeFraKilden: String,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
) {
    fun toDomain(): TiltaksdeltakelseSomFratarRett {
        return TiltaksdeltakelseSomFratarRett(
            eksternDeltakelseId = eksternDeltakelseId,
            typeNavn = typeNavn,
            tittel = tittel,
            tiltakskodeFraKilden = tiltakskodeFraKilden,
            periode = lukketEllerÅpenPeriode(deltakelseFraOgMed, deltakelseTilOgMed),
            deltakelseStatus = deltakelseStatus.toTiltakDeltakerstatus(),
            deltakelseProsent = deltakelseProsent,
            antallDagerPerUke = antallDagerPerUke,
            kilde = kilde.toTiltakskilde(),
        )
    }
}

fun TiltaksdeltakelseSomFratarRett.toDbJson(): TiltaksdeltakelseSomFratarRettDbJson {
    return TiltaksdeltakelseSomFratarRettDbJson(
        eksternDeltakelseId = eksternDeltakelseId,
        typeNavn = typeNavn,
        tittel = tittel,
        tiltakskodeFraKilden = tiltakskodeFraKilden,
        deltakelseFraOgMed = periode.fraOgMed,
        deltakelseTilOgMed = periode.tilOgMed,
        deltakelseStatus = deltakelseStatus.toDb(),
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
        kilde = kilde.toDb(),
    )
}
