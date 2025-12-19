package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode

data class InnvilgelsesperiodeKommando(
    val periode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val tiltaksdeltakelseId: String,
)
