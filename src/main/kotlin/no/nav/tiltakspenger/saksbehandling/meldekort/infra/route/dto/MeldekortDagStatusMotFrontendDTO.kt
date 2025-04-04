package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeUtfylt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret

enum class MeldekortDagStatusMotFrontendDTO {
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

fun MeldeperiodeBeregningDag.tilMeldekortDagStatusDTO(): MeldekortDagStatusMotFrontendDTO =
    when (this) {
        is Sperret -> MeldekortDagStatusMotFrontendDTO.SPERRET
        is IkkeUtfylt -> MeldekortDagStatusMotFrontendDTO.IKKE_UTFYLT
        is DeltattMedLønnITiltaket -> MeldekortDagStatusMotFrontendDTO.DELTATT_MED_LØNN_I_TILTAKET
        is DeltattUtenLønnITiltaket -> MeldekortDagStatusMotFrontendDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYKT_BARN
        is VelferdGodkjentAvNav -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        is VelferdIkkeGodkjentAvNav -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        is IkkeDeltatt -> MeldekortDagStatusMotFrontendDTO.IKKE_DELTATT
    }

fun MeldekortDagStatus.tilMeldekortDagStatusDTO(): MeldekortDagStatusMotFrontendDTO =
    when (this) {
        MeldekortDagStatus.SPERRET -> MeldekortDagStatusMotFrontendDTO.SPERRET
        MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatusMotFrontendDTO.IKKE_UTFYLT
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusMotFrontendDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusMotFrontendDTO.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatusMotFrontendDTO.IKKE_DELTATT
        MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    }
