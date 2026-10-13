package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomFratarRett
import java.time.LocalDate

/**
 * Lagres som en del av saksopplysningene.
 */
data class TiltaksdeltakelseSomFratarRettDbJson(
    val eksternDeltakelseId: String,
    val typeNavn: String,
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
            tiltakskodeFraKilden = tiltakskodeFraKilden,
            deltakelseFraOgMed = deltakelseFraOgMed,
            deltakelseTilOgMed = deltakelseTilOgMed,
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
        tiltakskodeFraKilden = tiltakskodeFraKilden,
        deltakelseFraOgMed = deltakelseFraOgMed,
        deltakelseTilOgMed = deltakelseTilOgMed,
        deltakelseStatus = deltakelseStatus.toDb(),
        deltakelseProsent = deltakelseProsent,
        antallDagerPerUke = antallDagerPerUke,
        kilde = kilde.toDb(),
    )
}
