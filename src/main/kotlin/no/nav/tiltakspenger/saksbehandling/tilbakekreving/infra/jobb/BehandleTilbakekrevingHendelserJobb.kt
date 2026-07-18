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
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
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
                hendelse.håndter().onLeft { (feil, sakId) ->
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

    private fun Tilbakekrevingshendelse.håndter(): Either<Pair<TilbakekrevinghendelseFeil, SakId?>, Unit> {
        if (this is TilbakekrevingUkjentHendelse) {
            return this.håndter()
        }

        val sak = this.eksternFagsakId?.let { saksnummerStr ->
            val saksnummer = Either.catch { Saksnummer(saksnummerStr) }.getOrElse {
                if (erFakeSak(saksnummerStr)) {
                    logger.info { "Sletter tilbakekreving-hendelse ${this.id} med fake-saksnummer $saksnummerStr" }
                    tilbakekrevingHendelseRepo.slett(this.id)
                    return Unit.right()
                }

                return Pair(TilbakekrevinghendelseFeil.UgyldigSaksnummer, null).left()
            }

            val sak = sakRepo.hentForSaksnummer(saksnummer)

            if (sak == null && erDev) {
                logger.info { "Sletter tilbakekreving-hendelse ${this.id} med ukjent saksnummer $saksnummerStr" }
                tilbakekrevingHendelseRepo.slett(this.id)
                return Unit.right()
            }

            sak
        } ?: return Pair(TilbakekrevinghendelseFeil.FantIkkeSak, null).left()

        return when (this) {
            is TilbakekrevingBehandlingEndretHendelse -> this.håndter(sak)
            is TilbakekrevingInfoBehovHendelse -> this.håndter(sak)
        }
    }

    /**
     * For en hendelse som tidligere ikke kunne deserialiseres - forsøk på nytt.
     * Hvis det går bra, oppdaterer vi raden i databasen til den korrekte hendelse-typen, slik at den blir behandlet normalt ved neste jobbkjøring.
     */
    private fun TilbakekrevingUkjentHendelse.håndter(): Either<Pair<TilbakekrevinghendelseFeil, SakId?>, Unit> {
        val hendelseId = this.id
        val oppdatertHendelse = this.value.tilNyTilbakekrevingshendelse(hendelseId)

        if (oppdatertHendelse == null || oppdatertHendelse is TilbakekrevingUkjentHendelse) {
            logger.warn { "Ukjent tilbakekreving-hendelse $hendelseId kunne fortsatt ikke deserialiseres - hopper over" }
            return Unit.right()
        }

        logger.info { "Deserialiserte tidligere ukjent tilbakekreving-hendelse $hendelseId til ${oppdatertHendelse.hendelsestype} - oppdaterer raden" }

        tilbakekrevingHendelseRepo.oppdaterUkjent(oppdatertHendelse)

        return Unit.right()
    }

    private fun TilbakekrevingInfoBehovHendelse.håndter(sak: Sak): Either<Pair<TilbakekrevinghendelseFeil, SakId?>, Unit> {
        val hendelseId = this.id
        val sakId = sak.id

        val utbetaling = sak.utbetalinger.hentUtbetalingForUuid(this.kravgrunnlagReferanse)
            ?: return Pair(TilbakekrevinghendelseFeil.FantIkkeUtbetaling, sakId).left()

        val infoSvarDTO = when (utbetaling.beregningKilde) {
            is BeregningKilde.BeregningKildeMeldekort ->
                sak.meldekortbehandlinger.hentMeldekortbehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(this)

            is BeregningKilde.BeregningKildeRammebehandling ->
                sak.rammebehandlinger.hentRammebehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(this)
        } ?: return Pair(TilbakekrevinghendelseFeil.FantIkkeBehandling, sakId).left()

        logger.info { "Produserer svar på tilbakekreving info-behov $hendelseId for sak $sakId med kravgrunnlagReferanse ${this.kravgrunnlagReferanse}" }

        tilbakekrevingProducer.produserInfoSvar(hendelseId, infoSvarDTO).also {
            tilbakekrevingHendelseRepo.markerInfoBehovSomBehandlet(hendelseId, sakId, it)
        }

        return Unit.right()
    }

    private fun TilbakekrevingBehandlingEndretHendelse.håndter(sak: Sak): Either<Pair<TilbakekrevinghendelseFeil, SakId?>, Unit> {
        val hendelseId = this.id
        val sakId = sak.id

        // Dette burde ikke kunne skje
        if (eksternBehandlingId == null) {
            logger.error { "Hendelse $hendelseId mangler eksternBehandlingId!" }
            return Pair(TilbakekrevinghendelseFeil.FantIkkeUtbetaling, sakId).left()
        }

        val utbetaling: VedtattUtbetaling? = eksternBehandlingId
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
            return Pair(TilbakekrevinghendelseFeil.FantIkkeUtbetaling, sakId).left()
        }

        val eksisterendeBehandling =
            tilbakekrevingBehandlingRepo.hentForTilbakeBehandlingId(tilbakeBehandlingId)

        val oppdatertEllerNyBehandling = if (eksisterendeBehandling != null) {
            val oppdatertBehandling = oppdaterBehandlingHvisEndret(eksisterendeBehandling)

            if (oppdatertBehandling == null) {
                tilbakekrevingHendelseRepo.markerEndringSomBehandlet(id, sakId)
                return Unit.right()
            }

            oppdatertBehandling
        } else {
            TilbakekrevingBehandling(
                id = TilbakekrevingId.random(),
                sakId = sak.id,
                utbetalingId = utbetaling.id,
                opprettet = this.sakOpprettet,
                sistEndret = this.opprettet,
                tilbakeBehandlingId = this.tilbakeBehandlingId,
                status = this.behandlingsstatus,
                url = this.url,
                kravgrunnlagTotalPeriode = this.fullstendigPeriode,
                totaltFeilutbetaltBeløp = this.totaltFeilutbetaltBeløp,
                varselSendt = this.varselSendt,
                saksbehandler = null,
                beslutter = null,
                venter = this.venter,
            )
        }

        logger.info { "Lagrer tilbakekrevingbehandling ${oppdatertEllerNyBehandling.id} for sak $sakId basert på hendelse $hendelseId" }

        sessionFactory.withTransactionContext { tx ->
            tilbakekrevingBehandlingRepo.lagre(oppdatertEllerNyBehandling, tx)
            tilbakekrevingHendelseRepo.markerEndringSomBehandlet(hendelseId, sakId, tx)
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

        private const val FAKE_SAK_PREFIX = "BF"
        private val erDev: Boolean = Configuration.isDev()

        // Team tilbake sender noen ganger saker de har generert selv for å teste i dev
        private fun erFakeSak(eksternSakId: String): Boolean {
            return erDev && eksternSakId.startsWith(FAKE_SAK_PREFIX)
        }
    }
}
