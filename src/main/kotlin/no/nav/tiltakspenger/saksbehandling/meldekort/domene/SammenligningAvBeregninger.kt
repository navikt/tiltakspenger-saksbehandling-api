package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskUkedagOgDatoUtenÅrFormatter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeUtfylt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SammenligningAvBeregninger.DagSammenligning

data class SammenligningAvBeregninger(
    val meldeperiode: List<SammenligningPerMeldeperiode>,
) {
    data class SammenligningPerMeldeperiode(
        val periode: Periode,
        val dager: List<DagSammenligning>,
    )

    data class DagSammenligning(
        val dato: String,
        val status: NyOgForrige<String>,
        val beløp: NyOgForrige<Int>,
        val barnetillegg: NyOgForrige<Int>,
        val prosent: NyOgForrige<Int>,
    )

    data class NyOgForrige<T>(
        val forrige: T?,
        val ny: T,
    )

    data class MeldeperiodeFørEtter(
        val meldeperiode: Meldeperiode,
        val beregningsdager: List<Beregningsdag>,
    )
}

fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregning?,
    nyBeregning: MeldeperiodeBeregning,
): SammenligningAvBeregninger.SammenligningPerMeldeperiode {
    if (forrigeBeregning == null) {
        return SammenligningAvBeregninger.SammenligningPerMeldeperiode(
            periode = nyBeregning.periode,
            dager = nyBeregning.dager.map {
                DagSammenligning(
                    dato = it.dato.format(norskUkedagOgDatoUtenÅrFormatter),
                    status = SammenligningAvBeregninger.NyOgForrige(
                        forrige = null,
                        ny = it.toStatus().toString(),
                    ),
                    beløp = SammenligningAvBeregninger.NyOgForrige(
                        forrige = null,
                        ny = it.beløp,
                    ),
                    barnetillegg = SammenligningAvBeregninger.NyOgForrige(
                        forrige = null,
                        ny = it.beløpBarnetillegg,
                    ),
                    prosent = SammenligningAvBeregninger.NyOgForrige(
                        forrige = null,
                        ny = it.prosent,
                    ),
                )
            },
        )
    }

    require(forrigeBeregning.periode == nyBeregning.periode) { "Periodene må være like" }

    return SammenligningAvBeregninger.SammenligningPerMeldeperiode(
        periode = forrigeBeregning.periode,
        dager = forrigeBeregning.dager
            .zip(nyBeregning.dager)
            .map { (forrige, ny) -> sammenlign(forrige, ny) },
    )
}

private fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregningDag,
    nyBeregning: MeldeperiodeBeregningDag,
): DagSammenligning {
    require(forrigeBeregning.dato == nyBeregning.dato) { "Datoene må være like" }

    return DagSammenligning(
        dato = forrigeBeregning.dato.format(norskUkedagOgDatoUtenÅrFormatter),
        status = SammenligningAvBeregninger.NyOgForrige(
            forrige = forrigeBeregning.toStatus().toString(),
            ny = nyBeregning.toStatus().toString(),
        ),
        beløp = SammenligningAvBeregninger.NyOgForrige(
            forrige = forrigeBeregning.beløp,
            ny = nyBeregning.beløp,
        ),
        barnetillegg = SammenligningAvBeregninger.NyOgForrige(
            forrige = forrigeBeregning.beløpBarnetillegg,
            ny = nyBeregning.beløpBarnetillegg,
        ),
        prosent = SammenligningAvBeregninger.NyOgForrige(
            forrige = forrigeBeregning.prosent,
            ny = nyBeregning.prosent,
        ),
    )
}

private fun MeldeperiodeBeregningDag.toStatus(): MeldekortDagStatus =
    when (this) {
        is IkkeUtfylt -> MeldekortDagStatus.IKKE_UTFYLT
        is DeltattMedLønnITiltaket -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        is DeltattUtenLønnITiltaket -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatus.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        is VelferdGodkjentAvNav -> MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        is VelferdIkkeGodkjentAvNav -> MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        is IkkeDeltatt -> MeldekortDagStatus.IKKE_DELTATT
        is Sperret -> MeldekortDagStatus.SPERRET
    }
