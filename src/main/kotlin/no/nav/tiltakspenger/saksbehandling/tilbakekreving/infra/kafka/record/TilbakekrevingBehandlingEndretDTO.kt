package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingshendelseId
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime

data class TilbakekrevingBehandlingEndretDTO(
    override val versjon: Int,
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: Tilbakekreving,
) : TilbakekrevingshendelseDTO {
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
            tilbakekrevingBehandlingId = tilbakekreving.behandlingId,
            sakOpprettet = tilbakekreving.sakOpprettet,
            varselSendt = tilbakekreving.varselSendt,
            behandlingsstatus = tilbakekreving.behandlingsstatus,
            totaltFeilutbetaltBeløp = tilbakekreving.totaltFeilutbetaltBeløp,
            url = tilbakekreving.saksbehandlingURL,
            fullstendigPeriode = tilbakekreving.fullstendigPeriode.tilPeriode(),
        )
    }

    data class Tilbakekreving(
        val behandlingId: String,
        val sakOpprettet: LocalDateTime,
        val varselSendt: LocalDateTime?,
        // Vet ikke hvilke verdier denne kan ha ennå, så setter den til String inntil videre
        val behandlingsstatus: String,
        val totaltFeilutbetaltBeløp: BigDecimal,
        val saksbehandlingURL: String,
        val fullstendigPeriode: TilbakekrevingPeriodeDTO,
    )

    enum class Behandlingsstatus {
        TIL_BEHANDLING,
    }
}
