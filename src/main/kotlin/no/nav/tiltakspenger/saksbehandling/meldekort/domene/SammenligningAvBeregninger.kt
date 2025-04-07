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
    val meldeperiode: List<MeldeperiodeSammenligninger>,
) {
    data class MeldeperiodeSammenligninger(
        val periode: Periode,
        val dager: List<DagSammenligning>,
    )

    data class DagSammenligning(
        val dato: String,
        val status: ForrigeOgGjeldende<String>,
        val beløp: ForrigeOgGjeldende<Int>,
        val barnetillegg: ForrigeOgGjeldende<Int>,
        val prosent: ForrigeOgGjeldende<Int>,
    )

    /**
     * Holder på forrige og gjeldende verdi for en gitt type. Gjeldende verdi er enten nåværende tilstand
     * eller verdien som ble gjeldende etter en endring
     */
    data class ForrigeOgGjeldende<T>(
        val forrige: T?,
        val gjeldende: T,
    )
}

fun sammenlign(
    forrigeBeregning: MeldekortBeregning.MeldeperiodeBeregnet?,
    nyBeregning: MeldekortBeregning.MeldeperiodeBeregnet,
): SammenligningAvBeregninger.MeldeperiodeSammenligninger {
    // I de fleste cases er det ikke gjort noen korrigering, dermed er det bare nåtilstand som gjelder og det vil ikke finnes noen  "forrige" beregning
    if (forrigeBeregning == null) {
        return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
            periode = nyBeregning.periode,
            dager = nyBeregning.dager.map {
                DagSammenligning(
                    dato = it.dato.format(norskUkedagOgDatoUtenÅrFormatter),
                    status = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.toStatus().toString(),
                    ),
                    beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.beløp,
                    ),
                    barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.beløpBarnetillegg,
                    ),
                    prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
                        forrige = null,
                        gjeldende = it.prosent,
                    ),
                )
            },
        )
    }
    // Hvis det er gjort en korrigering, så må vi sammenligne forrige og gjeldende beregning (det vil si de korrigerte verdiene)
    require(forrigeBeregning.periode == nyBeregning.periode) { "Periodene må være like" }

    return SammenligningAvBeregninger.MeldeperiodeSammenligninger(
        periode = forrigeBeregning.periode,
        dager = forrigeBeregning.dager
            .zip(nyBeregning.dager)
            .map { (forrige, gjeldende) -> sammenlign(forrige, gjeldende) },
    )
}

private fun sammenlign(
    forrigeBeregning: MeldeperiodeBeregningDag,
    nyBeregning: MeldeperiodeBeregningDag,
): DagSammenligning {
    require(forrigeBeregning.dato == nyBeregning.dato) { "Datoene må være like" }

    return DagSammenligning(
        dato = forrigeBeregning.dato.format(norskUkedagOgDatoUtenÅrFormatter),
        status = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.toStatus().toString(),
            gjeldende = nyBeregning.toStatus().toString(),
        ),
        beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.beløp,
            gjeldende = nyBeregning.beløp,
        ),
        barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.beløpBarnetillegg,
            gjeldende = nyBeregning.beløpBarnetillegg,
        ),
        prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
            forrige = forrigeBeregning.prosent,
            gjeldende = nyBeregning.prosent,
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
        is Sperret -> MeldekortDagStatus.IKKE_RETT
    }

enum class MeldekortDagStatus {
    IKKE_RETT,
    IKKE_UTFYLT,
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    IKKE_DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
}
