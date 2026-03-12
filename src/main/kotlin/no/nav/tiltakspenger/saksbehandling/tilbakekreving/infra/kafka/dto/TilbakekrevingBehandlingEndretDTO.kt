package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingshendelseId
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingBehandlingEndretDTO(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: TilbakekrevingDTO,
) : TilbakekrevingshendelseDTO {
    override val versjon: Int = 1
    override val hendelsestype = TilbakekrevingHendelsestypeDTO.behandling_endret

    override fun tilNyHendelse(
        key: String,
        clock: Clock,
    ): TilbakekrevingBehandlingEndretHendelse {
        return TilbakekrevingBehandlingEndretHendelse(
            id = TilbakekrevingshendelseId.random(),
            opprettet = nå(clock),
            behandlet = null,
            sakId = null,
            eksternFagsakId = eksternFagsakId,
            eksternBehandlingId = eksternBehandlingId,
            tilbakeBehandlingId = UUID.fromString(tilbakekreving.behandlingId),
            sakOpprettet = tilbakekreving.sakOpprettet,
            varselSendt = tilbakekreving.varselSendt,
            behandlingsstatus = tilbakekreving.behandlingsstatus.tilDomene(),
            totaltFeilutbetaltBeløp = tilbakekreving.totaltFeilutbetaltBeløp,
            url = tilbakekreving.saksbehandlingURL,
            fullstendigPeriode = tilbakekreving.fullstendigPeriode.tilPeriode(),
        )
    }

    data class TilbakekrevingDTO(
        val behandlingId: String,
        val sakOpprettet: LocalDateTime,
        val varselSendt: LocalDate?,
        val behandlingsstatus: TilbakekrevingBehandlingsstatusDTO,
        val forrigeBehandlingsstatus: TilbakekrevingBehandlingsstatusDTO?,
        val totaltFeilutbetaltBeløp: BigDecimal,
        val saksbehandlingURL: String,
        val fullstendigPeriode: TilbakekrevingPeriodeDTO,
    )

    enum class TilbakekrevingBehandlingsstatusDTO {
        OPPRETTET,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
        ;

        fun tilDomene(): TilbakekrevingBehandlingsstatus {
            return when (this) {
                OPPRETTET -> TilbakekrevingBehandlingsstatus.OPPRETTET
                TIL_BEHANDLING -> TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
                TIL_GODKJENNING -> TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
                AVSLUTTET -> TilbakekrevingBehandlingsstatus.AVSLUTTET
            }
        }
    }
}
