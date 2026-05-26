package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
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
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingUkjentHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.tilBehandlingIdFraTilbakekreving
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingProducer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingMottaker
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingRevurdering
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingUtvidPeriode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.tilNyTilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
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
        val ubehandledeHendelser = Either.catch { tilbakekrevingHendelseRepo.hentUbehandledeHendelser() }.getOrElse {
            logger.error(it) { "Feil ved henting av ubehandlede tilbakekreving-hendelser" }
            return
        }

        logger.debug { "Behandler ${ubehandledeHendelser.size} tilbakekreving-hendelser" }

        ubehandledeHendelser.forEach { hendelse ->
            Either.catch {
                when (hendelse) {
                    is TilbakekrevingInfoBehovHendelse -> håndterInfoBehov(hendelse)
                    is TilbakekrevingBehandlingEndretHendelse -> håndterBehandlingEndret(hendelse)
                    is TilbakekrevingUkjentHendelse -> håndterUkjent(hendelse)
                }.onLeft { (sakId, feil) ->
                    logger.error { "Feil ved behandling av tilbakekreving-hendelse: $feil - hendelse: ${hendelse.id} / ${hendelse.hendelsestype} - sak: $sakId" }
                    tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(hendelse.id, sakId, feil)
                }
            }.onLeft {
                logger.error(it) {
                    "Ukjent feil ved behandling av tilbakekreving-hendelse ${hendelse.id} / ${hendelse.hendelsestype}"
                }
            }
        }
    }

    /**
     * Slår opp [Sak] for en hendelse basert på dens eksternFagsakId.
     */
    private fun hentSakForHendelse(hendelse: Tilbakekrevingshendelse): Either<TilbakekrevinghendelseFeil, Pair<SakId, Sak>> {
        val eksternFagsakId = hendelse.eksternFagsakId

        val sakId = eksternFagsakId?.let {
            Either.catch { sakRepo.hentSakIdForSaksnummer(Saksnummer(it)) }.getOrElse { e ->
                logger.error(e) { "Feil ved oppslag av sak for eksternFagsakId $it - hendelse ${hendelse.id}" }
                null
            }
        } ?: return TilbakekrevinghendelseFeil.FantIkkeSak.left()

        return (sakId to sakRepo.hentForSakId(sakId)!!).right()
    }

    /**
     * For en hendelse som tidligere ikke kunne deserialiseres - forsøk på nytt. Hvis det går bra,
     * oppdaterer vi raden i databasen til den korrekte hendelse-typen, slik at den blir behandlet
     * normalt ved neste jobbkjøring.
     */
    private fun håndterUkjent(hendelse: TilbakekrevingUkjentHendelse): Either<Pair<SakId?, TilbakekrevinghendelseFeil>, Unit> {
        val oppdatertHendelse = hendelse.value.tilNyTilbakekrevingshendelse()

        if (oppdatertHendelse == null || oppdatertHendelse is TilbakekrevingUkjentHendelse) {
            logger.warn { "Ukjent tilbakekreving-hendelse ${hendelse.id} kunne fortsatt ikke deserialiseres - hopper over" }
            return Unit.right()
        }

        logger.info { "Deserialiserte tidligere ukjent tilbakekreving-hendelse ${hendelse.id} til ${oppdatertHendelse.hendelsestype} - oppdaterer raden" }

        val medEksisterendeId: Tilbakekrevingshendelse = when (oppdatertHendelse) {
            is TilbakekrevingBehandlingEndretHendelse -> oppdatertHendelse.copy(id = hendelse.id)
            is TilbakekrevingInfoBehovHendelse -> oppdatertHendelse.copy(id = hendelse.id)
        }

        tilbakekrevingHendelseRepo.oppdaterUkjent(medEksisterendeId)

        return Unit.right()
    }

    private fun håndterInfoBehov(hendelse: TilbakekrevingInfoBehovHendelse): Either<Pair<SakId?, TilbakekrevinghendelseFeil>, Unit> {
        val (sakId, sak) = hentSakForHendelse(hendelse).getOrElse { return (null to it).left() }

        val utbetaling = sak.utbetalinger.hentUtbetalingForUuid(hendelse.kravgrunnlagReferanse)
            ?: return (sakId to TilbakekrevinghendelseFeil.FantIkkeUtbetaling).left()

        val infoSvarDTO = when (utbetaling.beregningKilde) {
            is BeregningKilde.BeregningKildeMeldekort ->
                sak.meldekortbehandlinger.hentMeldekortbehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(hendelse)

            is BeregningKilde.BeregningKildeRammebehandling ->
                sak.rammebehandlinger.hentRammebehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(hendelse)
        } ?: return (sakId to TilbakekrevinghendelseFeil.FantIkkeBehandling).left()

        logger.info { "Produserer svar på tilbakekreving info-behov ${hendelse.id} for sak $sakId med kravgrunnlagReferanse ${hendelse.kravgrunnlagReferanse}" }

        tilbakekrevingProducer.produserInfoSvar(hendelse.id, infoSvarDTO).also {
            tilbakekrevingHendelseRepo.markerInfoBehovSomBehandlet(hendelse.id, sakId, it)
        }

        return Unit.right()
    }

    private fun håndterBehandlingEndret(hendelse: TilbakekrevingBehandlingEndretHendelse): Either<Pair<SakId?, TilbakekrevinghendelseFeil>, Unit> {
        val (sakId, sak) = hentSakForHendelse(hendelse).getOrElse { return (null to it).left() }

        // Dette burde ikke kunne skje
        if (hendelse.eksternBehandlingId == null) {
            logger.error { "Hendelse ${hendelse.id} mangler eksternBehandlingId!" }
            return (sakId to TilbakekrevinghendelseFeil.FantIkkeUtbetaling).left()
        }

        val utbetaling: VedtattUtbetaling? = hendelse.eksternBehandlingId
            .tilBehandlingIdFraTilbakekreving()
            .fold(
                ifLeft = {
                    sak.utbetalinger.hentUtbetalingForUuid(it)
                },
                ifRight = {
                    when (it) {
                        is RammebehandlingId -> sak.utbetalinger.hentUtbetalingForRammebehandling(it)
                        is MeldekortId -> sak.utbetalinger.hentUtbetalingForMeldekort(it)
                        else -> throw IllegalArgumentException("Ugyldig behandlingId: $it")
                    }
                },
            )

        if (utbetaling == null) {
            return (sakId to TilbakekrevinghendelseFeil.FantIkkeUtbetaling).left()
        }

        val eksisterendeBehandling =
            tilbakekrevingBehandlingRepo.hentForTilbakeBehandlingId(hendelse.tilbakeBehandlingId)

        val oppdatertEllerNyBehandling = if (eksisterendeBehandling != null) {
            val oppdatertBehandling = hendelse.oppdaterBehandlingHvisEndret(eksisterendeBehandling)

            if (oppdatertBehandling == null) {
                tilbakekrevingHendelseRepo.markerEndringSomBehandlet(hendelse.id, sakId)
                return Unit.right()
            }

            oppdatertBehandling
        } else {
            TilbakekrevingBehandling(
                id = TilbakekrevingId.random(),
                sakId = sak.id,
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
                venter = hendelse.venter,
            )
        }

        logger.info { "Lagrer tilbakekrevingbehandling ${oppdatertEllerNyBehandling.id} for sak $sakId basert på hendelse ${hendelse.id}" }

        sessionFactory.withTransactionContext { tx ->
            tilbakekrevingBehandlingRepo.lagre(oppdatertEllerNyBehandling, tx)
            tilbakekrevingHendelseRepo.markerEndringSomBehandlet(hendelse.id, sakId, tx)
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
            hendelseOpprettet = nå(clock).toString(),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = id.toString(),
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
            behandlendeEnhet = NAV_TILTAK_OSLO_ENHET,
        )
    }

    private fun Rammebehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.status == Rammebehandlingsstatus.VEDTATT) {
            "Rammebehandlingen må være vedtatt for å produsere svar på info-behov - id: $id, status: $status"
        }

        return TilbakekrevingInfoSvarDTO(
            eksternFagsakId = behov.eksternFagsakId,
            hendelseOpprettet = nå(clock).toString(),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = id.toString(),
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
            behandlendeEnhet = NAV_TILTAK_OSLO_ENHET,
        )
    }

    companion object {
        private const val NAV_TILTAK_OSLO_ENHET = "0387"
    }
}
