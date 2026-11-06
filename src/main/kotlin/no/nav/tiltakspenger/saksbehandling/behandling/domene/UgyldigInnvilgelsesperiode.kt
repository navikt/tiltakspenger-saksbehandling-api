package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * En innvilgelsesperiode som ikke passer med tiltaksdeltakelsen den innvilges for i saksopplysningene.
 *
 * Kan oppstå når tiltaksdeltakelsen endres etter at saksbehandler fylte ut innvilgelsen.
 * Vi nullstiller ikke innvilgelsen når saksopplysningene oppdateres, men lar saksbehandler rette den selv.
 * Behandlingen kan derfor ha en ugyldig innvilgelse mens den er under behandling, men den kan ikke lagres, sendes til beslutning eller iverksettes.
 */
sealed interface UgyldigInnvilgelsesperiode {
    val periode: Periode
    val internDeltakelseId: TiltaksdeltakerId

    fun beskrivelse(): String

    data class TiltaksdeltakelsenFinnesIkke(
        override val periode: Periode,
        override val internDeltakelseId: TiltaksdeltakerId,
    ) : UgyldigInnvilgelsesperiode {
        override fun beskrivelse() =
            "Tiltaksdeltakelsen $internDeltakelseId som er valgt for perioden $periode finnes ikke i saksopplysningene"
    }

    /** Tiltaksdeltakelsen mangler start- eller sluttdato, eller har en status som ikke gir rett, f.eks. at deltakelsen ikke har startet. */
    data class TiltaksdeltakelsenKanIkkeInnvilges(
        override val periode: Periode,
        override val internDeltakelseId: TiltaksdeltakerId,
    ) : UgyldigInnvilgelsesperiode {
        override fun beskrivelse() =
            "Tiltaksdeltakelsen $internDeltakelseId som er valgt for perioden $periode kan ikke innvilges"
    }

    data class TiltaksdeltakelsenDekkerIkkePerioden(
        override val periode: Periode,
        override val internDeltakelseId: TiltaksdeltakerId,
        val deltakelsesperiode: Periode,
    ) : UgyldigInnvilgelsesperiode {
        override fun beskrivelse() =
            "Tiltaksdeltakelsen $internDeltakelseId ($deltakelsesperiode) dekker ikke hele perioden $periode"
    }
}

/** @return null dersom tiltaksdeltakelsen med [internDeltakelseId] kan innvilges for hele [periode] */
fun Tiltaksdeltakelser.validerInnvilgelsesperiode(
    periode: Periode,
    internDeltakelseId: TiltaksdeltakerId,
): UgyldigInnvilgelsesperiode? {
    val tiltaksdeltakelse = getTiltaksdeltakelse(internDeltakelseId)
        ?: return UgyldigInnvilgelsesperiode.TiltaksdeltakelsenFinnesIkke(periode, internDeltakelseId)

    val deltakelsesperiode = tiltaksdeltakelse.lukketPeriode
    if (!tiltaksdeltakelse.kanInnvilges || deltakelsesperiode == null) {
        return UgyldigInnvilgelsesperiode.TiltaksdeltakelsenKanIkkeInnvilges(periode, internDeltakelseId)
    }

    if (!deltakelsesperiode.inneholderHele(periode)) {
        return UgyldigInnvilgelsesperiode.TiltaksdeltakelsenDekkerIkkePerioden(periode, internDeltakelseId, deltakelsesperiode)
    }

    return null
}

/** @return innvilgelsesperiodene som ikke passer med [tiltaksdeltakelser], eller en tom liste dersom alle er gyldige */
fun IkkeTomPeriodisering<InnvilgelsesperiodeKommando>.ugyldigePerioder(
    tiltaksdeltakelser: Tiltaksdeltakelser,
): List<UgyldigInnvilgelsesperiode> {
    return perioderMedVerdi.mapNotNull {
        tiltaksdeltakelser.validerInnvilgelsesperiode(it.periode, it.verdi.internDeltakelseId)
    }
}
