package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelseKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * @param tiltaksdeltakelser Tiltaksdeltakelsene det innvilges for i perioden, minst én.
 * Den første bestemmer tiltakstypen for perioden.
 * @param erReversertOpphør Perioden innvilges uten gyldig tiltaksdeltakelse, på nytt etter å ha vært opphørt eller stanset.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 */
data class InnvilgelsesperiodeDTO(
    val periode: PeriodeDTO,
    val tiltaksdeltakelser: List<InnvilgetTiltaksdeltakelseDTO>,
    val erReversertOpphør: Boolean = false,
)

/**
 * @param ugyldigTiltaksdeltakelse Settes bare i responser, og er null dersom tiltaksdeltakelsen passer med perioden.
 * Ignoreres i requester.
 * Er alltid null for reversert opphør, siden perioden da ikke valideres mot tiltaksdeltakelsene i saksopplysningene.
 */
data class InnvilgetTiltaksdeltakelseDTO(
    val internDeltakelseId: String,
    val antallDagerPerMeldeperiode: Int,
    val ugyldigTiltaksdeltakelse: UgyldigTiltaksdeltakelseDTO? = null,
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
                tiltaksdeltakelser = it.tiltaksdeltakelser.map { tiltaksdeltakelse ->
                    InnvilgetTiltaksdeltakelseKommando(
                        internDeltakelseId = TiltaksdeltakerId.fromString(tiltaksdeltakelse.internDeltakelseId),
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(tiltaksdeltakelse.antallDagerPerMeldeperiode),
                    )
                }.toNonEmptyListOrThrow(),
                erReversertOpphør = it.erReversertOpphør,
            ),
        )
    }.tilIkkeTomPeriodisering()
}

/** @param tiltaksdeltakelser Tiltaksdeltakelsene fra saksopplysningene som innvilgelsesperiodene valideres mot. */
fun Innvilgelsesperioder.tilDTO(tiltaksdeltakelser: Tiltaksdeltakelser): InnvilgelsesperioderDTO {
    return this.periodisering.perioderMedVerdi.map { (verdi, periode) ->
        InnvilgelsesperiodeDTO(
            periode = periode.toDTO(),
            tiltaksdeltakelser = verdi.tiltaksdeltakelser.map {
                InnvilgetTiltaksdeltakelseDTO(
                    internDeltakelseId = it.internDeltakelseId.toString(),
                    antallDagerPerMeldeperiode = it.antallDagerPerMeldeperiode.value,
                    ugyldigTiltaksdeltakelse = if (verdi.erReversertOpphør) {
                        null
                    } else {
                        tiltaksdeltakelser.validerInnvilgelsesperiode(periode, it.internDeltakelseId)?.tilDTO()
                    },
                )
            },
            erReversertOpphør = verdi.erReversertOpphør,
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
