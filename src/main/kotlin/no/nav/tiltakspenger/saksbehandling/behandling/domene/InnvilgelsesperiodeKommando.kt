package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * @param tiltaksdeltakelser Tiltaksdeltakelsene det innvilges for i perioden.
 * Den første bestemmer tiltakstypen for perioden, se [InnvilgelsesperiodeVerdi.førsteTiltakstype].
 * @param erReversertOpphør Saksbehandler har valgt å innvilge perioden som et reversert opphør, uten gyldig tiltaksdeltakelse.
 * Slike perioder konverteres med [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.tilInnvilgelsesperioderMedReversertOpphør], og aldri med [tilInnvilgelsesperioder].
 */
data class InnvilgelsesperiodeKommando(
    val periode: Periode,
    val tiltaksdeltakelser: NonEmptyList<InnvilgetTiltaksdeltakelseKommando>,
    val erReversertOpphør: Boolean = false,
) {
    val internDeltakelseIder: NonEmptyList<TiltaksdeltakerId> get() = tiltaksdeltakelser.map { it.internDeltakelseId }

    /** Summen av antall dager per meldeperiode for tiltaksdeltakelsene, se [summer]. */
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode get() = tiltaksdeltakelser.map { it.antallDagerPerMeldeperiode }.summer()

    init {
        require(internDeltakelseIder.distinct().size == internDeltakelseIder.size) {
            "En innvilgelsesperiode kan ikke ha samme tiltaksdeltakelse flere ganger: $internDeltakelseIder"
        }
    }
}

data class InnvilgetTiltaksdeltakelseKommando(
    val internDeltakelseId: TiltaksdeltakerId,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
)

/** Periodene der summen av antall dager per meldeperiode for tiltaksdeltakelsene er mer enn [MAKS_DAGER_FOR_MELDEPERIODE]. */
fun IkkeTomPeriodisering<InnvilgelsesperiodeKommando>.perioderMedForMangeDager(): NonEmptyList<Periode>? {
    return perioderMedVerdi
        .filter { it.verdi.tiltaksdeltakelser.map { innvilget -> innvilget.antallDagerPerMeldeperiode }.overstigerMaksForMeldeperiode() }
        .map { it.periode }
        .toNonEmptyListOrNull()
}

/** Om noen av periodene er markert som reversert opphør. */
val IkkeTomPeriodisering<InnvilgelsesperiodeKommando>.harReversertOpphør: Boolean
    get() = verdier.any { it.erReversertOpphør }

/**
 * Konverterer innvilgelsesperioder for gyldige tiltaksdeltakelser.
 * Kaster dersom noen av periodene er markert som reversert opphør.
 */
fun IkkeTomPeriodisering<InnvilgelsesperiodeKommando>.tilInnvilgelsesperioder(behandling: Rammebehandling): Innvilgelsesperioder {
    require(!harReversertOpphør) {
        "Innvilgelsesperioder som er reversert opphør må konverteres med perioder for reversert opphør for ${behandling.id}"
    }
    return Innvilgelsesperioder(
        this.map { it.verdi.tilInnvilgelsesperiodeVerdi(behandling) },
    )
}

/** Slår opp tiltaksdeltakelsene i saksopplysningene til [behandling], for en periode som ikke er reversert opphør. */
fun InnvilgelsesperiodeKommando.tilInnvilgelsesperiodeVerdi(behandling: Rammebehandling): InnvilgelsesperiodeVerdi {
    require(!erReversertOpphør) {
        "Innvilgelsesperioden $periode er reversert opphør, og har ikke tiltaksdeltakelsen fra saksopplysningene for ${behandling.id}"
    }

    return InnvilgelsesperiodeVerdi(
        tiltaksdeltakelser = tiltaksdeltakelser.map {
            val tiltaksdeltakelse = behandling.getTiltaksdeltakelse(it.internDeltakelseId)

            requireNotNull(tiltaksdeltakelse) {
                "Fant ikke tiltaket ${it.internDeltakelseId} i saksopplysningene for ${behandling.id}"
            }

            InnvilgetTiltaksdeltakelse(
                tiltaksdeltakelse = tiltaksdeltakelse,
                antallDagerPerMeldeperiode = it.antallDagerPerMeldeperiode,
            )
        },
        reversertOpphør = null,
    )
}
