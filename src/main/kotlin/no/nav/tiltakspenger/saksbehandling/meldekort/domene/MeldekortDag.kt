package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Sperret
import java.time.LocalDate

data class MeldekortDag(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
) {
    val harDeltattEllerFravær = when (status) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_SYK,
        MeldekortDagStatus.FRAVÆR_SYKT_BARN,
        MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV,
        MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
        -> true

        MeldekortDagStatus.SPERRET,
        MeldekortDagStatus.IKKE_UTFYLT,
        MeldekortDagStatus.IKKE_DELTATT,
        -> false
    }
}

enum class MeldekortDagStatus {
    SPERRET,
    IKKE_UTFYLT,
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    IKKE_DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
}

fun MeldeperiodeBeregningDag.tilMeldekortDagStatus(): MeldekortDagStatus =
    when (this) {
        is DeltattMedLønnITiltaket -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        is DeltattUtenLønnITiltaket -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatus.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        is VelferdGodkjentAvNav -> MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        is VelferdIkkeGodkjentAvNav -> MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        is IkkeDeltatt -> MeldekortDagStatus.IKKE_DELTATT
        is Sperret -> MeldekortDagStatus.SPERRET
    }
