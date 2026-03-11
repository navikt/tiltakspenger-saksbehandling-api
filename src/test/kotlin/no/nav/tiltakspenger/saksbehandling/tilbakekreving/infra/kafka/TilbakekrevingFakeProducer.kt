package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingshendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingBehandlingEndretDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock

class TilbakekrevingFakeProducer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) : TilbakekrevingProducer {

    override fun produserInfoSvar(
        behovHendelseId: TilbakekrevingshendelseId,
        infoSvar: TilbakekrevingInfoSvarDTO,
    ): String {
        sakRepo.hentForSaksnummer(Saksnummer(infoSvar.eksternFagsakId))
            ?.sendBehandlingEndretHendelse(behovHendelseId, infoSvar)

        return serialize(infoSvar)
    }

    private fun Sak.sendBehandlingEndretHendelse(
        behovHendelseId: TilbakekrevingshendelseId,
        infoSvar: TilbakekrevingInfoSvarDTO,
    ) {
        val (periode, simulering) = utbetalinger
            .hentUtbetalingForUuid(infoSvar.revurdering.behandlingId)
            ?.beregningKilde?.let { beregningKilde ->
                when (beregningKilde) {
                    is BeregningKilde.BeregningKildeBehandling -> hentRammebehandling(beregningKilde.id)?.let {
                        it.vedtaksperiode!! to it.utbetaling?.simulering
                    }

                    is BeregningKilde.BeregningKildeMeldekort -> hentMeldekortBehandling(beregningKilde.id)?.let {
                        it.periode to it.simulering
                    }
                }
            } ?: return

        val behandlingEndretDTO = TilbakekrevingBehandlingEndretDTO(
            eksternFagsakId = infoSvar.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            eksternBehandlingId = infoSvar.revurdering.behandlingId,
            tilbakekreving = TilbakekrevingBehandlingEndretDTO.Tilbakekreving(
                behandlingId = infoSvar.revurdering.behandlingId,
                sakOpprettet = nå(clock),
                varselSendt = nå(clock),
                behandlingsstatus = "OPPRETTET",
                totaltFeilutbetaltBeløp = (
                    (simulering as? Simulering.Endring)?.totalFeilutbetaling
                        ?: 0
                    ).toBigDecimal(),
                saksbehandlingURL = "http://tilbake-fake-url/fagsystem/TP/fagsak/${infoSvar.eksternFagsakId}/behandling/${behovHendelseId.uuidPart()}",
                fullstendigPeriode = TilbakekrevingPeriodeDTO(
                    fom = periode.fraOgMed,
                    tom = periode.tilOgMed,
                ),
            ),
        )

        TilbakekrevingConsumer.consume(
            behovHendelseId.uuidPart(),
            serialize(behandlingEndretDTO),
            clock,
            tilbakekrevingHendelseRepo,
        )
    }
}
