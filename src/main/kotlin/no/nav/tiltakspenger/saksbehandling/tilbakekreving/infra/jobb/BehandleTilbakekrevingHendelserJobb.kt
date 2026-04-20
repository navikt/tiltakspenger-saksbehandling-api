package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
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
    private val sakRepo: SakRepo,
    private val tilbakekrevingProducer: TilbakekrevingProducer,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    fun håndterUbehandledeHendelser() {
        logger.debug { "Kjører jobb for å behandle tilbakekreving-hendelser" }

        val ubehandledeHendelser = Either.catch { tilbakekrevingHendelseRepo.hentUbehandledeHendelser() }.getOrElse {
            logger.error(it) { "Feil ved henting av ubehandlede tilbakekreving-hendelser" }
            return
        }

        ubehandledeHendelser.forEach { hendelse ->
            val sakId = hendelse.sakId

            if (sakId == null) {
                logger.error { "Tilbakekreving-hendelse ${hendelse.id} er ikke knyttet til en sak, og kan derfor ikke behandles" }
                tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(
                    hendelse.id,
                    TilbakekrevinghendelseFeil.FantIkkeSak,
                )
                return@forEach
            }

            val sak = sakRepo.hentForSakId(sakId)!!

            Either.catch {
                when (hendelse) {
                    is TilbakekrevingInfoBehovHendelse -> sak.håndterInfoBehov(hendelse)
                    is TilbakekrevingBehandlingEndretHendelse -> sak.håndterBehandlingEndret(hendelse)
                }.onLeft {
                    logger.error { "Feil ved behandling av tilbakekreving-hendelse: $it - hendelse: ${hendelse.id} / ${hendelse.hendelsestype} - sak: ${hendelse.sakId}" }
                    tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(
                        hendelse.id,
                        it,
                    )
                }
            }.onLeft {
                logger.error(it) {
                    "Ukjent feil ved behandling av tilbakekreving-hendelse ${hendelse.id} / ${hendelse.hendelsestype} på sak ${hendelse.sakId}"
                }
            }
        }
    }

    private fun Sak.håndterInfoBehov(hendelse: TilbakekrevingInfoBehovHendelse): Either<TilbakekrevinghendelseFeil, Unit> {
        val utbetaling = utbetalinger.hentUtbetalingForUuid(hendelse.kravgrunnlagReferanse)
            ?: return TilbakekrevinghendelseFeil.FantIkkeUtbetaling.left()

        val infoSvarDTO = when (utbetaling.beregningKilde) {
            is BeregningKilde.BeregningKildeMeldekort ->
                meldekortbehandlinger.hentMeldekortbehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(hendelse)

            is BeregningKilde.BeregningKildeRammebehandling ->
                rammebehandlinger.hentRammebehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(hendelse)
        }

        if (infoSvarDTO == null) {
            return TilbakekrevinghendelseFeil.FantIkkeBehandling.left()
        }

        logger.info { "Produserer svar på tilbakekreving info-behov ${hendelse.id} for sak $id med kravgrunnlagReferanse ${hendelse.kravgrunnlagReferanse}" }

        tilbakekrevingProducer.produserInfoSvar(hendelse.id, infoSvarDTO).also {
            tilbakekrevingHendelseRepo.markerInfoBehovSomBehandlet(hendelse.id, it)
        }

        return Unit.right()
    }

    private fun Sak.håndterBehandlingEndret(hendelse: TilbakekrevingBehandlingEndretHendelse): Either<TilbakekrevinghendelseFeil, Unit> {
        val utbetaling = hendelse.eksternBehandlingId?.let { utbetalinger.hentUtbetalingForUuid(it) }

        if (utbetaling == null) {
            return TilbakekrevinghendelseFeil.FantIkkeUtbetaling.left()
        }

        val eksisterendeBehandling =
            tilbakekrevingBehandlingRepo.hentForTilbakeBehandlingId(hendelse.tilbakeBehandlingId)

        val oppdatertEllerNyBehandling = if (eksisterendeBehandling != null) {
            val oppdatertBehandling = hendelse.oppdaterBehandlingHvisEndret(eksisterendeBehandling)

            if (oppdatertBehandling == null) {
                tilbakekrevingHendelseRepo.markerEndringSomBehandlet(hendelse.id)
                return Unit.right()
            }

            oppdatertBehandling
        } else {
            TilbakekrevingBehandling(
                id = TilbakekrevingId.random(),
                sakId = this.id,
                utbetalingId = utbetaling.id,
                opprettet = hendelse.sakOpprettet,
                sistEndret = hendelse.opprettet,
                tilbakeBehandlingId = hendelse.tilbakeBehandlingId,
                status = hendelse.behandlingsstatus,
                url = hendelse.url,
                kravgrunnlagTotalPeriode = hendelse.fullstendigPeriode,
                totaltFeilutbetaltBeløp = hendelse.totaltFeilutbetaltBeløp,
                varselSendt = hendelse.varselSendt,
                saksbehandler = null,
                beslutter = null,
            )
        }

        logger.info { "Lagrer tilbakekrevingbehandling ${oppdatertEllerNyBehandling.id} for sak $id basert på hendelse ${hendelse.id}" }

        sessionFactory.withTransactionContext { tx ->
            tilbakekrevingBehandlingRepo.lagre(oppdatertEllerNyBehandling, tx)
            tilbakekrevingHendelseRepo.markerEndringSomBehandlet(hendelse.id, tx)
        }

        return Unit.right()
    }

    private fun Meldekortbehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.erGodkjent) {
            "Meldekortet må være godkjent for å produsere svar på info-behov - id: $id, status: $status"
        }

        if (this.status == MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET) {
            logger.error { "Automatisk behandlet meldekort $id har ført til en tilbakekrevingssak (hendelse id ${behov.id}). Dette burde ikke kunne skje." }
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
            // TODO: dette er nok ikke riktig, avventer tilbakemelding fra team tilbake
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
            behandlendeEnhet = NAV_TILTAK_OSLO_ENHET,
        )
    }

    private fun Rammebehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.status == Rammebehandlingsstatus.VEDTATT) {
            "Rammebehandlingen må være vedtatt for å produsere svar på info-behov - id: $id, status: $status"
        }

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
            // TODO: dette er nok ikke riktig, avventer tilbakemelding fra team tilbake
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
            behandlendeEnhet = NAV_TILTAK_OSLO_ENHET,
        )
    }

    companion object {
        private const val NAV_TILTAK_OSLO_ENHET = "0387"
    }
}
