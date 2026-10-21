package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat

data class OppdaterRammebehandlingForBeslutterKommando(
    val sakId: SakId,
    val behandlingId: RammebehandlingId,
    val beslutter: Saksbehandler,
    val skalJournalføreNotat: SkalJournalføreNotat,
    val correlationId: CorrelationId,
)
