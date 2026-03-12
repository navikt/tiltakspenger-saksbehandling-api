package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingProducer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingMottaker
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingRevurdering
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingUtvidPeriode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import java.time.Clock

class BehandleTilbakekrevingHendelserJobb(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val tilbakekrevingBehandlingRepo: TilbakekrevingBehandlingRepo,
    private val sakService: SakService,
    private val tilbakekrevingProducer: TilbakekrevingProducer,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    fun behandleTilbakekrevingInfoBehov() {
        logger.debug { "Kjører jobb for å behandle tilbakekreving info behov" }

        val infoBehovHendelser = Either.catch { tilbakekrevingHendelseRepo.hentUbehandledeInfoBehov() }.getOrElse {
            logger.error(it) { "Feil ved henting av ubehandlede info behov" }
            return
        }

        infoBehovHendelser.forEach { behovHendelse ->
            Either.catch {
                val sak =
                    Either.catch { sakService.hentForSaksnummer(Saksnummer(behovHendelse.eksternFagsakId)) }.getOrElse {
                        logger.error(it) { "Fant ingen sak for saksnummer/eksternFagsakId ${behovHendelse.eksternFagsakId} fra behovHendelse ${behovHendelse.id}" }
                        tilbakekrevingHendelseRepo.oppdaterBehandletInfoBehovFeil(
                            behovHendelse.id,
                            "Fant ikke sak for saksnummer",
                        )
                        return@forEach
                    }

                val utbetaling = sak.utbetalinger.hentUtbetalingForUuid(behovHendelse.kravgrunnlagReferanse)

                if (utbetaling == null) {
                    logger.error { "Fant ingen utbetaling for kravgrunnlagReferanse/uuid ${behovHendelse.kravgrunnlagReferanse} i sak ${sak.id}" }
                    tilbakekrevingHendelseRepo.oppdaterBehandletInfoBehovFeil(
                        behovHendelse.id,
                        "Fant ikke utbetaling for kravgrunnlagReferanse",
                    )
                    return@forEach
                }

                val infoSvarDTO = when (utbetaling.beregningKilde) {
                    is BeregningKilde.BeregningKildeMeldekort ->
                        sak.meldekortbehandlinger.hentMeldekortBehandling(utbetaling.beregningKilde.id)
                            ?.tilSvarDTO(behovHendelse)

                    is BeregningKilde.BeregningKildeBehandling ->
                        sak.rammebehandlinger.hentRammebehandling(utbetaling.beregningKilde.id)
                            ?.tilSvarDTO(behovHendelse)
                }

                if (infoSvarDTO == null) {
                    logger.error { "Fant ingen behandling for beregningskilde ${utbetaling.beregningKilde.id} i sak ${sak.id}" }
                    tilbakekrevingHendelseRepo.oppdaterBehandletInfoBehovFeil(
                        behovHendelse.id,
                        "Fant ikke behandling for utbetalingens beregningskilde",
                    )
                    return@forEach
                }

                logger.info { "Produserer svar på tilbakekreving info-behov ${behovHendelse.id} for sak ${sak.id} med kravgrunnlagReferanse ${behovHendelse.kravgrunnlagReferanse}" }
                tilbakekrevingProducer.produserInfoSvar(behovHendelse.id, infoSvarDTO).also {
                    tilbakekrevingHendelseRepo.oppdaterBehandletInfoBehovMedSvar(behovHendelse.id, it)
                }
            }.onLeft {
                logger.error(it) { "Feil ved behandling av tilbakekreving info-behov ${behovHendelse.id}" }
            }
        }
    }

    fun behandleTilbakekrevingBehandlingEndret() {
        logger.debug { "Kjører jobb for å behandle tilbakekreving behandling endret" }

        val behandlingEndretHendelser =
            Either.catch { tilbakekrevingHendelseRepo.hentUbehandledeEndringer() }.getOrElse {
                logger.error(it) { "Feil ved henting av ubehandlede behandling endret hendelser" }
                return
            }

        behandlingEndretHendelser.forEach { hendelse ->
            Either.catch {
                val sak = Either.catch { sakService.hentForSaksnummer(Saksnummer(hendelse.eksternFagsakId)) }
                    .getOrElse {
                        logger.error(it) { "Fant ingen sak for saksnummer/eksternFagsakId ${hendelse.eksternFagsakId} fra behandlingEndretHendelse ${hendelse.id}" }
                        tilbakekrevingHendelseRepo.oppdaterBehandletEndringFeil(
                            hendelse.id,
                            "Fant ikke sak for saksnummer",
                        )
                        return@forEach
                    }

                val utbetaling = hendelse.utbetalingId?.let { sak.utbetalinger.hentUtbetaling(it) }

                if (utbetaling == null) {
                    logger.error { "Fant ingen utbetaling for id ${hendelse.utbetalingId} i sak ${sak.id}" }
                    tilbakekrevingHendelseRepo.oppdaterBehandletEndringFeil(
                        hendelse.id,
                        "Fant ikke utbetaling for tilbakeBehandlingId",
                    )
                    return@forEach
                }

                val tilbakekrevingBehandling = TilbakekrevingBehandling(
                    id = TilbakekrevingId.random(),
                    sakId = sak.id,
                    utbetalingId = utbetaling.id,
                    tilbakeBehandlingId = hendelse.tilbakeBehandlingId,
                    opprettet = nå(clock),
                    status = hendelse.behandlingsstatus,
                    url = hendelse.url,
                    kravgrunnlagTotalPeriode = hendelse.fullstendigPeriode,
                    totaltFeilutbetaltBeløp = hendelse.totaltFeilutbetaltBeløp,
                    varselSendt = hendelse.varselSendt,
                )

                logger.info { "Lagrer tilbakekrevingbehandling ${tilbakekrevingBehandling.id} for sak ${sak.id} basert på hendelse ${hendelse.id}" }

                sessionFactory.withTransactionContext { tx ->
                    tilbakekrevingBehandlingRepo.lagre(tilbakekrevingBehandling, tx)
                    tilbakekrevingHendelseRepo.oppdaterBehandletEndring(hendelse.id, tx)
                }
            }.onLeft {
                logger.error(it) { "Feil ved behandling av tilbakekreving behandling-endret ${hendelse.id}" }
            }
        }
    }

    private fun MeldekortBehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.status == MeldekortBehandlingStatus.GODKJENT) {
            "Meldekortet må være godkjent for å kunne produsere et svar på info-behov. MeldekortBehandlingId: ${this.id}, status: ${this.status}"
        }

        return TilbakekrevingInfoSvarDTO(
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
            behandlendeEnhet = this.navkontor.kontornummer,
        )
    }

    private fun Rammebehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        return TilbakekrevingInfoSvarDTO(
            eksternFagsakId = behov.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = behov.kravgrunnlagReferanse,
                årsak = TilbakekrevingInfoSvarDTO.TilbakekrevingRevurderingÅrsak.NYE_OPPLYSNINGER,
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
            behandlendeEnhet = this.utbetaling!!.navkontor.kontornummer,
        )
    }
}
