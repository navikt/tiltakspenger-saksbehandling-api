package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ValgtBarnetillegg
import java.time.LocalDate

/**
 * Forhåndsviser vedtaksbrevet for en forlengelse med verdiene saksbehandler har i skjemaet.
 * Perioden og barnetillegget utledes som ved oppdatering av forlengelsen.
 *
 * @param barnetillegg Null betyr at barnetillegget utledes fra vedtaket som forlenges.
 */
data class ForhåndsvisVedtaksbrevForForlengelseKommando(
    override val sakId: SakId,
    override val behandlingId: RammebehandlingId,
    override val correlationId: CorrelationId,
    override val saksbehandler: Saksbehandler,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val tilOgMed: LocalDate,
    val barnetillegg: ValgtBarnetillegg?,
) : ForhåndsvisVedtaksbrevForRevurderingKommando
