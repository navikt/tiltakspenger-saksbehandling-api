package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDate

data class TiltaksdeltakelseDb(
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: String,
    val deltagelseFraOgMed: LocalDate?,
    val deltagelseTilOgMed: LocalDate?,
    val deltakelseStatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
    val rettPåTiltakspenger: Boolean,
    val deltidsprosentGjennomforing: Double? = null,
    val internDeltakelseId: String? = null,
) {
    fun toDomain(): Tiltaksdeltakelse {
        return Tiltaksdeltakelse(
            eksternDeltakelseId = eksternDeltagelseId,
            gjennomføringId = gjennomføringId,
            typeNavn = typeNavn,
            typeKode = typeKode.toTiltakstypeSomGirRett(),
            deltakelseFraOgMed = deltagelseFraOgMed,
            deltakelseTilOgMed = deltagelseTilOgMed,
            deltakelseStatus = deltakelseStatus.toTiltakDeltakerstatus(),
            deltakelseProsent = deltakelseProsent,
            antallDagerPerUke = antallDagerPerUke,
            kilde = kilde.toTiltakskilde(),
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = internDeltakelseId,
        )
    }
}

fun Tiltaksdeltakelse.toDbJson(): TiltaksdeltakelseDb {
    return TiltaksdeltakelseDb(
        eksternDeltagelseId = this.eksternDeltakelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.toDb(),
        deltagelseFraOgMed = this.deltakelseFraOgMed,
        deltagelseTilOgMed = this.deltakelseTilOgMed,
        deltakelseStatus = this.deltakelseStatus.toDb(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.toDb(),
        rettPåTiltakspenger = this.rettPåTiltakspenger,
        deltidsprosentGjennomforing = this.deltidsprosentGjennomforing,
        internDeltakelseId = internDeltakelseId,
    )
}
