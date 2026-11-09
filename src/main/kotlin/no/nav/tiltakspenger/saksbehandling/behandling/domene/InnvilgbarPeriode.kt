package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

/**
 * En periode saksbehandler kan innvilge for, sammen med tiltaksdeltakelsen det innvilges for.
 * Perioder for ulike tiltaksdeltakelser kan overlappe.
 */
data class InnvilgbarPeriode(
    val internDeltakelseId: TiltaksdeltakerId,
    val periode: Periode,
)

/**
 * Periodene det kan innvilges for, sortert på periode.
 * Dette er den eneste kilden til hvilke perioder som kan innvilges.
 * Både valideringen i [validerInnvilgelsesperiode] og frontend bygger på den.
 * I dag kan hele perioden til hver tiltaksdeltakelse som [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern.kanInnvilges] innvilges.
 */
fun Tiltaksdeltakelser.innvilgbarePerioder(): List<InnvilgbarPeriode> {
    return this
        .filter { it.kanInnvilges }
        .mapNotNull { tiltaksdeltakelse ->
            tiltaksdeltakelse.lukketPeriode?.let { InnvilgbarPeriode(tiltaksdeltakelse.internDeltakelseId, it) }
        }
        .sortedWith(compareBy({ it.periode.fraOgMed }, { it.periode.tilOgMed }))
}
