package no.nav.tiltakspenger.saksbehandling.behandling.domene.barnetillegg

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

data class OppdaterBarnetilleggCommand(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val innvilgelsesperiode: Periode,
    val barnetillegg: Barnetillegg?,
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
    val tiltaksdeltakelser: List<Pair<Periode, String>>,
) {
    fun valgteTiltaksdeltakelser(behandling: Behandling): ValgteTiltaksdeltakelser = ValgteTiltaksdeltakelser.periodiser(
        tiltaksdeltakelser = tiltaksdeltakelser,
        behandling = behandling,
    )
}
