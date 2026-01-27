package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class StartSøknadsbehandlingPåNyttKommando(
    val sakId: SakId,
    val søknadId: SøknadId,
    val klagebehandlingId: KlagebehandlingId?,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val søknadsbehandlingId: BehandlingId = BehandlingId.random(),
)
