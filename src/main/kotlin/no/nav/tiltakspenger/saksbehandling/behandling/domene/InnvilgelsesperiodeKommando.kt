package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * @param internDeltakelseIder Tiltaksdeltakelsene det innvilges for i perioden.
 * Den første bestemmer tiltakstypen for perioden, se [InnvilgelsesperiodeVerdi.førsteTiltakstype].
 * @param antallDagerPerMeldeperiode Antall dager per meldeperiode som innvilges i perioden, felles for alle tiltaksdeltakelsene.
 * @param erReversertOpphør Saksbehandler har valgt å innvilge perioden som et reversert opphør, uten gyldig tiltaksdeltakelse.
 * Slike perioder konverteres med [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.tilInnvilgelsesperioderMedReversertOpphør], og aldri med [tilInnvilgelsesperioder].
 */
data class InnvilgelsesperiodeKommando(
    val periode: Periode,
    val internDeltakelseIder: NonEmptyList<TiltaksdeltakerId>,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val erReversertOpphør: Boolean = false,
) {
    init {
        require(internDeltakelseIder.distinct().size == internDeltakelseIder.size) {
            "En innvilgelsesperiode kan ikke ha samme tiltaksdeltakelse flere ganger: $internDeltakelseIder"
        }
    }
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
        tiltaksdeltakelser = internDeltakelseIder.map {
            requireNotNull(behandling.getTiltaksdeltakelse(it)) {
                "Fant ikke tiltaket $it i saksopplysningene for ${behandling.id}"
            }
        },
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        reversertOpphør = null,
    )
}
