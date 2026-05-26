package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto

import no.nav.tiltakspenger.saksbehandling.felles.tilLocalDateTime
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingVenter
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingVenter.TilbakekrevingVentegrunn
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import java.math.BigDecimal
import java.time.LocalDate

data class TilbakekrevingBehandlingEndretDTO(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: String,
    val eksternBehandlingId: String?,
    val tilbakekreving: TilbakekrevingDTO,
) : TilbakekrevingshendelseDTO {
    override val versjon: Int = 1
    override val hendelsestype = TilbakekrevingHendelsestypeDTO.behandling_endret

    override fun tilHendelseForLagring(): TilbakekrevingBehandlingEndretHendelse {
        return TilbakekrevingBehandlingEndretHendelse(
            id = TilbakekrevinghendelseId.random(),
            opprettet = hendelseOpprettet.tilLocalDateTime(),
            behandlet = null,
            sakId = null,
            eksternFagsakId = eksternFagsakId,
            eksternBehandlingId = eksternBehandlingId,
            tilbakeBehandlingId = tilbakekreving.behandlingId,
            sakOpprettet = tilbakekreving.sakOpprettet.tilLocalDateTime(),
            varselSendt = tilbakekreving.varselSendt,
            behandlingsstatus = tilbakekreving.behandlingsstatus.tilDomene(),
            forrigeBehandlingsstatus = tilbakekreving.forrigeBehandlingsstatus?.tilDomene(),
            totaltFeilutbetaltBeløp = tilbakekreving.totaltFeilutbetaltBeløp,
            url = tilbakekreving.saksbehandlingURL,
            fullstendigPeriode = tilbakekreving.fullstendigPeriode.tilPeriode(),
            venter = tilbakekreving.venter?.tilDomene(),
            feil = null,
        )
    }

    data class TilbakekrevingDTO(
        val behandlingId: String,
        val sakOpprettet: String,
        val varselSendt: LocalDate?,
        val behandlingsstatus: TilbakekrevingHendelseStatusDTO,
        val forrigeBehandlingsstatus: TilbakekrevingHendelseStatusDTO?,
        val totaltFeilutbetaltBeløp: BigDecimal,
        val saksbehandlingURL: String,
        val fullstendigPeriode: TilbakekrevingPeriodeDTO,
        val venter: VenterDTO?,
    )

    data class VenterDTO(
        val grunn: VentegrunnDTO,
        val gjenopptas: LocalDate,
    ) {
        enum class VentegrunnDTO {
            AVVENTER_BRUKERUTTALELSE,
        }

        fun tilDomene(): TilbakekrevingVenter {
            return TilbakekrevingVenter(
                grunn = when (grunn) {
                    VentegrunnDTO.AVVENTER_BRUKERUTTALELSE -> TilbakekrevingVentegrunn.AVVENTER_BRUKERUTTALELSE
                },
                gjenopptas = gjenopptas,
            )
        }
    }

    enum class TilbakekrevingHendelseStatusDTO {
        OPPRETTET,
        TIL_FORHÅNDSVARSEL,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
        ;

        fun tilDomene(): TilbakekrevingBehandlingsstatus {
            return when (this) {
                OPPRETTET -> TilbakekrevingBehandlingsstatus.OPPRETTET
                TIL_FORHÅNDSVARSEL -> TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL
                TIL_BEHANDLING -> TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
                TIL_GODKJENNING -> TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
                AVSLUTTET -> TilbakekrevingBehandlingsstatus.AVSLUTTET
            }
        }
    }
}
