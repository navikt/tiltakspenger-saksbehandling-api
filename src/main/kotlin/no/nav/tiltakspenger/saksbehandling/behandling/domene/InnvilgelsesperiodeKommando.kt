package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

data class InnvilgelsesperiodeKommando(
    val periode: Periode,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode,
    val tiltaksdeltakelseId: String,
    val internDeltakelseId: TiltaksdeltakerId?,
)
