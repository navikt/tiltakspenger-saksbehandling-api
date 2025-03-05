package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.IkkeUtfylt
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret

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

fun MeldeperiodeBeregningDag.toStatusDTO(): MeldekortDagStatusMotFrontendDTO =
    when (this) {
        is IkkeUtfylt -> MeldekortDagStatusMotFrontendDTO.IKKE_UTFYLT
        is DeltattMedLønnITiltaket -> MeldekortDagStatusMotFrontendDTO.DELTATT_MED_LØNN_I_TILTAKET
        is DeltattUtenLønnITiltaket -> MeldekortDagStatusMotFrontendDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYKT_BARN
        is VelferdGodkjentAvNav -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        is VelferdIkkeGodkjentAvNav -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        is IkkeDeltatt -> MeldekortDagStatusMotFrontendDTO.IKKE_DELTATT
        is Sperret -> MeldekortDagStatusMotFrontendDTO.SPERRET
    }
