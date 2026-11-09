package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * @param erReversertOpphør Saksbehandler har valgt å innvilge perioden som et reversert opphør, uten gyldig tiltaksdeltakelse.
 * Slike perioder konverteres med [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.tilInnvilgelsesperioderMedReversertOpphør], og aldri med [tilInnvilgelsesperioder].
 */
data class InnvilgelsesperiodeKommando(
    val periode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val internDeltakelseId: TiltaksdeltakerId,
    val erReversertOpphør: Boolean = false,
)

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

/** Slår opp tiltaksdeltakelsen i saksopplysningene til [behandling], for en periode som ikke er reversert opphør. */
fun InnvilgelsesperiodeKommando.tilInnvilgelsesperiodeVerdi(behandling: Rammebehandling): InnvilgelsesperiodeVerdi {
    require(!erReversertOpphør) {
        "Innvilgelsesperioden $periode er reversert opphør, og har ikke tiltaksdeltakelsen fra saksopplysningene for ${behandling.id}"
    }

    val tiltaksdeltakelse = behandling.getTiltaksdeltakelse(internDeltakelseId)

    requireNotNull(tiltaksdeltakelse) {
        "Fant ikke tiltaket $internDeltakelseId i saksopplysningene for ${behandling.id}"
    }

    return InnvilgelsesperiodeVerdi(
        valgtTiltaksdeltakelse = tiltaksdeltakelse,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        reversertOpphør = null,
    )
}
