package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Sperret

enum class MeldekortDagStatusDTO {
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

fun MeldeperiodeBeregningDag.tilMeldekortDagStatusDTO(): MeldekortDagStatusDTO =
    when (this) {
        is Sperret -> MeldekortDagStatusDTO.SPERRET
        is DeltattMedLønnITiltaket -> MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
        is DeltattUtenLønnITiltaket -> MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatusDTO.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
        is VelferdGodkjentAvNav -> MeldekortDagStatusDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        is VelferdIkkeGodkjentAvNav -> MeldekortDagStatusDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        is IkkeDeltatt -> MeldekortDagStatusDTO.IKKE_DELTATT
    }

fun MeldekortDagStatus.tilMeldekortDagStatusDTO(): MeldekortDagStatusDTO =
    when (this) {
        MeldekortDagStatus.SPERRET -> MeldekortDagStatusDTO.SPERRET
        MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatusDTO.IKKE_UTFYLT
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatusDTO.IKKE_DELTATT
        MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDTO.FRAVÆR_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatusDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatusDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    }
