package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.LocalDate

/**
 * @param tittel Mangler i saksopplysninger og innvilgelsesperioder lagret før tittelen ble innført.
 */
data class TiltaksdeltakelseDb(
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val tittel: String?,
    val typeKode: String,
    val deltagelseFraOgMed: LocalDate?,
    val deltagelseTilOgMed: LocalDate?,
    val deltakelseStatus: String,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: String,
    val rettPåTiltakspenger: Boolean,
    val deltidsprosentGjennomforing: Double? = null,
    val internDeltakelseId: String,
) {
    fun toDomain(): TiltaksdeltakelseIntern {
        return TiltaksdeltakelseIntern(
            eksternDeltakelseId = eksternDeltagelseId,
            gjennomføringId = gjennomføringId,
            typeNavn = typeNavn,
            tittel = tittel,
            tiltakstype = typeKode.toTiltakstypeSomGirRett(),
            periode = lukketEllerÅpenPeriode(deltagelseFraOgMed, deltagelseTilOgMed),
            deltakelseStatus = deltakelseStatus.toTiltakDeltakerstatus(),
            deltakelseProsent = deltakelseProsent,
            antallDagerPerUke = antallDagerPerUke,
            kilde = kilde.toTiltakskilde(),
            rettPåTiltakspenger = rettPåTiltakspenger,
            deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            internDeltakelseId = TiltaksdeltakerId.fromString(internDeltakelseId),
        )
    }
}

fun TiltaksdeltakelseIntern.toDbJson(): TiltaksdeltakelseDb {
    return TiltaksdeltakelseDb(
        eksternDeltagelseId = this.eksternDeltakelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        tittel = this.tittel,
        typeKode = this.tiltakstype.toDb(),
        deltagelseFraOgMed = this.periode.fraOgMed,
        deltagelseTilOgMed = this.periode.tilOgMed,
        deltakelseStatus = this.deltakelseStatus.toDb(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.toDb(),
        rettPåTiltakspenger = this.rettPåTiltakspenger,
        deltidsprosentGjennomforing = this.deltidsprosentGjennomforing,
        internDeltakelseId = internDeltakelseId.toString(),
    )
}
