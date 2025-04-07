package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDateTime

data class MeldeperiodeKorrigering(
    val meldekortId: MeldekortId,
    val kjedeId: MeldeperiodeKjedeId,
    val periode: Periode,
    val iverksatt: LocalDateTime,
    val dager: NonEmptyList<MeldeperiodeBeregningDag.Utfylt>,
)
