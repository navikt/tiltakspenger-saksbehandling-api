package no.nav.tiltakspenger.meldekort.domene.v2

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.SakId

data class MeldeperiodeKjede(
    val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
}
