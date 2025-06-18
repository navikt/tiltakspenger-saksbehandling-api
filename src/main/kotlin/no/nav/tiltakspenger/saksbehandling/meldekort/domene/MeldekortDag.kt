package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeBesvart
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import java.time.LocalDate

data class MeldekortDag(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
) {
    /** Merk at denne gir true for [DeltattMedLønnITiltaket] og [FraværAnnet] som ikke gir rett til tiltakspenger */
    val harDeltattEllerFravær = when (status) {
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_SYK,
        MeldekortDagStatus.FRAVÆR_SYKT_BARN,
        MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV,
        MeldekortDagStatus.FRAVÆR_ANNET,
        -> true

        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.IKKE_TILTAKSDAG,
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
        -> false
    }
}

enum class MeldekortDagStatus {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,

    /** Kun et "valg" for bruker; ikke saksbehandler. Bruker har ikke tatt stilling til denne dagen. Het tidligere IKKE_REGISTRERT og IKKE_UFYLT. */
    IKKE_BESVART,

    /** Kun et valg for saksbehandler. Het tidligere IKKE_DELTATT */
    IKKE_TILTAKSDAG,

    /** Enten har bruker aldri fått innvilget denne dagen, eller så har den senere blitt stanset. */
    IKKE_RETT_TIL_TILTAKSPENGER,
}

fun MeldeperiodeBeregningDag.tilMeldekortDagStatus(): MeldekortDagStatus =
    when (this) {
        is DeltattUtenLønnITiltaket -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        is DeltattMedLønnITiltaket -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        is SykBruker -> MeldekortDagStatus.FRAVÆR_SYK
        is SyktBarn -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        is FraværGodkjentAvNav -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
        is FraværAnnet -> MeldekortDagStatus.FRAVÆR_ANNET
        is IkkeBesvart -> MeldekortDagStatus.IKKE_BESVART
        is IkkeDeltatt -> MeldekortDagStatus.IKKE_TILTAKSDAG
        is IkkeRettTilTiltakspenger -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }
