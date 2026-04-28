package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tilBehandlingIdFraTilbakekreving
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingBehandlingEndretDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingBehandlingEndretDTO.TilbakekrevingHendelseStatusDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.util.UUID

class TilbakekrevingFakeProducer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) : TilbakekrevingProducer {

    override fun produserInfoSvar(
        behovHendelseId: TilbakekrevinghendelseId,
        infoSvar: TilbakekrevingInfoSvarDTO,
    ): String {
        sakRepo.hentForSaksnummer(Saksnummer(infoSvar.eksternFagsakId))
            ?.sendBehandlingEndretHendelse(infoSvar)

        return serialize(infoSvar)
    }

    private fun Sak.sendBehandlingEndretHendelse(infoSvar: TilbakekrevingInfoSvarDTO) {
        val behandlingId = infoSvar.revurdering.behandlingId.tilBehandlingIdFraTilbakekreving().mapLeft {
            utbetalinger.hentUtbetalingForUuid(it)?.beregningKilde?.id
        }.getOrThrow()

        val (periode, simulering) = when (behandlingId) {
            is RammebehandlingId -> hentRammebehandling(behandlingId)?.let {
                it.vedtaksperiode!! to it.utbetaling?.simulering
            }

            is MeldekortId -> hentMeldekortbehandling(behandlingId)?.let {
                it.periode to it.simulering
            }

            else -> throw IllegalArgumentException()
        } ?: return

        val tilbakeBehandlingId = UUID.randomUUID().toString()

        val behandlingEndretDTO = TilbakekrevingBehandlingEndretDTO(
            eksternFagsakId = infoSvar.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            eksternBehandlingId = infoSvar.revurdering.behandlingId,
            tilbakekreving = TilbakekrevingBehandlingEndretDTO.TilbakekrevingDTO(
                behandlingId = tilbakeBehandlingId,
                sakOpprettet = nå(clock),
                varselSendt = null,
                behandlingsstatus = TilbakekrevingHendelseStatusDTO.OPPRETTET,
                forrigeBehandlingsstatus = null,
                totaltFeilutbetaltBeløp = (
                    (simulering as? Simulering.Endring)?.totalFeilutbetaling
                        ?: 0
                    ).toBigDecimal(),
                saksbehandlingURL = "http://tilbake-fake-url/fagsystem/TP/fagsak/${infoSvar.eksternFagsakId}/behandling/$tilbakeBehandlingId",
                fullstendigPeriode = TilbakekrevingPeriodeDTO(
                    fom = periode.fraOgMed,
                    tom = periode.tilOgMed,
                ),
            ),
        )

        TilbakekrevingConsumer.consume(
            this.fnr.verdi,
            serialize(behandlingEndretDTO),
            tilbakekrevingHendelseRepo,
            sakRepo,
        )
    }
}
