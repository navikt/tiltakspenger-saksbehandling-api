package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeBesvart
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger

enum class MeldekortDagStatusDTO {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,
    IKKE_BESVART,
    IKKE_TILTAKSDAG,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun MeldeperiodeBeregningDag.tilMeldekortDagStatusDTO(): MeldekortDagStatusDTO =
    when (this) {
        is DeltattUtenLønnITiltaket -> MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        is DeltattMedLønnITiltaket -> MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatusDTO.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
        is FraværGodkjentAvNav -> MeldekortDagStatusDTO.FRAVÆR_GODKJENT_AV_NAV
        is FraværAnnet -> MeldekortDagStatusDTO.FRAVÆR_ANNET
        is IkkeBesvart -> MeldekortDagStatusDTO.IKKE_BESVART
        is IkkeDeltatt -> MeldekortDagStatusDTO.IKKE_TILTAKSDAG
        is IkkeRettTilTiltakspenger -> MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
    }

fun MeldekortDagStatus.tilMeldekortDagStatusDTO(): MeldekortDagStatusDTO =
    when (this) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDTO.FRAVÆR_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatusDTO.FRAVÆR_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_ANNET -> MeldekortDagStatusDTO.FRAVÆR_ANNET
        MeldekortDagStatus.IKKE_BESVART -> MeldekortDagStatusDTO.IKKE_BESVART
        MeldekortDagStatus.IKKE_TILTAKSDAG -> MeldekortDagStatusDTO.IKKE_TILTAKSDAG
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
    }
