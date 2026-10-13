package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseSomIkkeGirRett
import java.time.LocalDate

/**
 * Lagres som en del av saksopplysningene.
 */
data class TiltaksdeltakelseSomIkkeGirRettDbJson(
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
    fun toDomain(): TiltaksdeltakelseSomIkkeGirRett {
        return TiltaksdeltakelseSomIkkeGirRett(
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

fun TiltaksdeltakelseSomIkkeGirRett.toDbJson(): TiltaksdeltakelseSomIkkeGirRettDbJson {
    return TiltaksdeltakelseSomIkkeGirRettDbJson(
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
