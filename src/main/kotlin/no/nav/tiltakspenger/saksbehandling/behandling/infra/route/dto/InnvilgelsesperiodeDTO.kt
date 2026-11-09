package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * @param ugyldigTiltaksdeltakelse Settes bare i responser, og er null dersom valgt tiltaksdeltakelse passer med perioden.
 * Ignoreres i requester.
 * Er alltid null for reversert opphør, siden perioden da ikke valideres mot tiltaksdeltakelsene i saksopplysningene.
 * @param erReversertOpphør Perioden innvilges uten gyldig tiltaksdeltakelse, på nytt etter å ha vært opphørt eller stanset.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 */
data class InnvilgelsesperiodeDTO(
    val periode: PeriodeDTO,
    val antallDagerPerMeldeperiode: Int,
    val internDeltakelseId: String,
    val ugyldigTiltaksdeltakelse: UgyldigTiltaksdeltakelseDTO? = null,
    val erReversertOpphør: Boolean = false,
)

/**
 * Valgt tiltaksdeltakelse passer ikke med innvilgelsesperioden i saksopplysningene, se [UgyldigInnvilgelsesperiode].
 * @param deltakelsesperiode Settes bare for [Type.DEKKER_IKKE_PERIODEN].
 */
data class UgyldigTiltaksdeltakelseDTO(
    val type: Type,
    val deltakelsesperiode: PeriodeDTO?,
) {
    enum class Type {
        FINNES_IKKE,
        KAN_IKKE_INNVILGES,
        DEKKER_IKKE_PERIODEN,
    }
}

typealias InnvilgelsesperioderDTO = List<InnvilgelsesperiodeDTO>

fun InnvilgelsesperioderDTO.tilKommando(): IkkeTomPeriodisering<InnvilgelsesperiodeKommando> {
    return this.map {
        val periode = it.periode.toDomain()

        PeriodeMedVerdi(
            periode = periode,
            verdi = InnvilgelsesperiodeKommando(
                periode = periode,
                antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                internDeltakelseId = TiltaksdeltakerId.fromString(it.internDeltakelseId),
                erReversertOpphør = it.erReversertOpphør,
            ),
        )
    }.tilIkkeTomPeriodisering()
}

/** @param tiltaksdeltakelser Tiltaksdeltakelsene fra saksopplysningene som innvilgelsesperiodene valideres mot. */
fun Innvilgelsesperioder.tilDTO(tiltaksdeltakelser: Tiltaksdeltakelser): InnvilgelsesperioderDTO {
    return this.periodisering.perioderMedVerdi.map {
        val internDeltakelseId = it.verdi.valgtTiltaksdeltakelse.internDeltakelseId

        InnvilgelsesperiodeDTO(
            periode = it.periode.toDTO(),
            antallDagerPerMeldeperiode = it.verdi.antallDagerPerMeldeperiode.value,
            internDeltakelseId = internDeltakelseId.toString(),
            ugyldigTiltaksdeltakelse = if (it.verdi.erReversertOpphør) {
                null
            } else {
                tiltaksdeltakelser.validerInnvilgelsesperiode(it.periode, internDeltakelseId)?.tilDTO()
            },
            erReversertOpphør = it.verdi.erReversertOpphør,
        )
    }
}

private fun UgyldigInnvilgelsesperiode.tilDTO(): UgyldigTiltaksdeltakelseDTO {
    return when (this) {
        is UgyldigInnvilgelsesperiode.TiltaksdeltakelsenFinnesIkke -> UgyldigTiltaksdeltakelseDTO(
            type = UgyldigTiltaksdeltakelseDTO.Type.FINNES_IKKE,
            deltakelsesperiode = null,
        )

        is UgyldigInnvilgelsesperiode.TiltaksdeltakelsenKanIkkeInnvilges -> UgyldigTiltaksdeltakelseDTO(
            type = UgyldigTiltaksdeltakelseDTO.Type.KAN_IKKE_INNVILGES,
            deltakelsesperiode = null,
        )

        is UgyldigInnvilgelsesperiode.TiltaksdeltakelsenDekkerIkkePerioden -> UgyldigTiltaksdeltakelseDTO(
            type = UgyldigTiltaksdeltakelseDTO.Type.DEKKER_IKKE_PERIODEN,
            deltakelsesperiode = deltakelsesperiode.toDTO(),
        )
    }
}
