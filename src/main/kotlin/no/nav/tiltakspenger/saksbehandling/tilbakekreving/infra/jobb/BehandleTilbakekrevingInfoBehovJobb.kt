package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingshendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record.TilbakekrevingInfoSvarDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record.TilbakekrevingInfoSvarDTO.TilbakekrevingMottaker
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record.TilbakekrevingInfoSvarDTO.TilbakekrevingRevurdering
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record.TilbakekrevingInfoSvarDTO.TilbakekrevingUtvidPeriode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record.TilbakekrevingPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelsePostgresRepo
import java.time.Clock

class BehandleTilbakekrevingInfoBehovJobb(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelsePostgresRepo,
    private val sakService: SakService,
    private val topic: String,
    private val clock: Clock,
    private val kafkaProducer: Producer<String, String> = Producer(KafkaConfigImpl()),
) {
    private val logger = KotlinLogging.logger {}

    fun behandleTilbakekrevingInfoBehov() {
        logger.debug { "Kjører jobb for å behandle tilbakekreving info behov" }

        val infoBehovHendelser = Either.catch { tilbakekrevingHendelseRepo.hentUbehandledeInfoBehov() }.getOrElse {
            logger.error(it) { "Feil ved henting av ubehandlede info behov" }
            return
        }

        infoBehovHendelser.forEach { hendelse ->
            Either.catch {
                val sak = sakService.hentForSaksnummer(Saksnummer(hendelse.eksternFagsakId))

                val utbetaling = sak.utbetalinger.hentUtbetalingForUuidPart(hendelse.kravgrunnlagReferanse)

                if (utbetaling == null) {
                    logger.error { "Fant ingen utbetaling for kravgrunnlagReferanse/uuid ${hendelse.kravgrunnlagReferanse} i sak ${sak.id}" }
                    return@forEach
                }

                val svarDto = when (utbetaling.beregningKilde) {
                    is BeregningKilde.BeregningKildeMeldekort ->
                        sak.meldekortbehandlinger.hentMeldekortBehandling(utbetaling.beregningKilde.id)
                            ?.tilSvarDTO(hendelse)

                    is BeregningKilde.BeregningKildeBehandling ->
                        sak.rammebehandlinger.hentRammebehandling(utbetaling.beregningKilde.id)?.tilSvarDTO(hendelse)
                }

                if (svarDto == null) {
                    logger.error { "Fant ingen behandling for beregningskids ${utbetaling.beregningKilde.id} i sak ${sak.id}" }
                    return@forEach
                }

                val svarJson = serialize(svarDto)

                logger.info { "Produserer svar på tilbakekreving info-behov for sak ${sak.id} med kravgrunnlagReferanse ${hendelse.kravgrunnlagReferanse}" }
//                kafkaProducer.produce(topic, svarDto.id.toString(), serialize(svarJson))
                tilbakekrevingHendelseRepo.oppdaterBehandletInfoBehov(svarDto.id, svarJson)
            }.onLeft {
                logger.error(it) { "Feil ved behandling av tilbakekreving info-behov ${hendelse.id}" }
            }
        }
    }

    private fun MeldekortBehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.status == MeldekortBehandlingStatus.GODKJENT) {
            "Meldekortet må være godkjent for å kunne produsere et svar på info-behov. MeldekortBehandlingId: ${this.id}, status: ${this.status}"
        }

        return TilbakekrevingInfoSvarDTO(
            id = TilbakekrevingshendelseId.random(),
            eksternFagsakId = behov.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = behov.kravgrunnlagReferanse,
                årsak = TilbakekrevingInfoSvarDTO.TilbakekrevingRevurderingÅrsak.KORRIGERING,
                årsakTilFeilutbetaling = this.begrunnelse?.verdi,
                vedtaksdato = this.iverksattTidspunkt!!.toLocalDate(),
            ),
            utvidPerioder = listOf(
                TilbakekrevingUtvidPeriode(
                    kravgrunnlagPeriode = TilbakekrevingPeriodeDTO(
                        fom = this.beregning!!.fraOgMed,
                        tom = this.beregning!!.tilOgMed,
                    ),
                    vedtaksperiode = TilbakekrevingPeriodeDTO(
                        fom = this.periode.fraOgMed,
                        tom = this.periode.tilOgMed,
                    ),
                ),
            ),
        )
    }

    private fun Rammebehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        return TilbakekrevingInfoSvarDTO(
            id = TilbakekrevingshendelseId.random(),
            eksternFagsakId = behov.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = behov.kravgrunnlagReferanse,
                årsak = TilbakekrevingInfoSvarDTO.TilbakekrevingRevurderingÅrsak.KORRIGERING,
                årsakTilFeilutbetaling = this.begrunnelseVilkårsvurdering?.verdi,
                vedtaksdato = this.iverksattTidspunkt!!.toLocalDate(),
            ),
            utvidPerioder = listOf(
                TilbakekrevingUtvidPeriode(
                    kravgrunnlagPeriode = TilbakekrevingPeriodeDTO(
                        fom = this.utbetaling!!.beregning.fraOgMed,
                        tom = this.utbetaling!!.beregning.tilOgMed,
                    ),
                    vedtaksperiode = TilbakekrevingPeriodeDTO(
                        fom = this.vedtaksperiode!!.fraOgMed,
                        tom = this.vedtaksperiode!!.tilOgMed,
                    ),
                ),
            ),
        )
    }
}
