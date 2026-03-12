package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingBehandlingEndretDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingBehandlingEndretHendelse(
    override val id: TilbakekrevingshendelseId,
    override val opprettet: LocalDateTime,
    override val behandlet: LocalDateTime?,
    override val sakId: SakId?,
    override val eksternFagsakId: String,
    val eksternBehandlingId: String?,
    val tilbakeBehandlingId: UUID,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val forrigeBehandlingsstatus: TilbakekrevingBehandlingsstatus?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val url: String,
    val fullstendigPeriode: Periode,
) : Tilbakekrevingshendelse {
    override val hendelsestype = TilbakekrevingHendelsestype.BehandlingEndret

    val utbetalingId: UtbetalingId? by lazy {
        if (eksternBehandlingId != null) {
            UtbetalingId.fromUUID(UUID.fromString(eksternBehandlingId))
        } else {
            null
        }
    }
}
