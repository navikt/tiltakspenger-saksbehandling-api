package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeRammebehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOpphør
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.harEndringer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

class SendRammebehandlingTilBeslutningService(
    private val sakService: SakService,
    private val simulerService: SimulerService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun sendTilBeslutning(
        kommando: SendBehandlingTilBeslutningKommando,
    ): Either<KanIkkeSendeRammebehandlingTilBeslutter, Pair<Sak, Rammebehandling>> {
        val sak = sakService.hentForSakId(
            sakId = kommando.sakId,
        )

        val behandling = sak.hentRammebehandling(kommando.behandlingId)

        requireNotNull(behandling) {
            "Fant ikke behandlingen ${kommando.behandlingId} på sak ${kommando.sakId}"
        }

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeSendeRammebehandlingTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
                .left()
        }

        val beregningOgSimulering = sak.beregnOgSimulerHvisAktuelt(behandling).getOrElse {
            return it.left()
        }

        if (behandling.utbetaling?.simulering.harEndringer(beregningOgSimulering?.first?.simulering)) {
            logger.error { "Utbetaling på behandlingen har endringer i simuleringen som vi ikke kan iverksette - ${kommando.behandlingId}" }
            return KanIkkeSendeRammebehandlingTilBeslutter.SimuleringEndret.left()
        }

        behandling.utbetaling?.also { utbetaling ->
            utbetaling.validerKanIverksetteUtbetaling().onLeft {
                logger.error { "Utbetaling på behandlingen har et resultat som vi ikke kan iverksette - ${kommando.behandlingId} / $it" }
                return KanIkkeSendeRammebehandlingTilBeslutter.UtbetalingStøttesIkke(it).left()
            }
        }

        return behandling.tilBeslutning(
            kommando = kommando,
            clock = clock,
        ).map {
            val oppdaterSak = sak.oppdaterRammebehandling(it)

            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)

            sessionFactory.withTransactionContext { tx ->
                rammebehandlingRepo.lagre(it, tx)
                rammebehandlingRepo.oppdaterSimuleringMetadata(
                    behandling.id,
                    beregningOgSimulering?.second?.originalResponseBody,
                    tx,
                )
                statistikkSakRepo.lagre(statistikk, tx)
            }

            oppdaterSak to it
        }
    }

    private suspend fun Sak.beregnOgSimulerHvisAktuelt(behandling: Rammebehandling): Either<KanIkkeSendeRammebehandlingTilBeslutter, Pair<BehandlingUtbetaling, SimuleringMedMetadata>?> {
        val vedtaksperiode = behandling.vedtaksperiode!!
        val behandlingId = behandling.id

        return when (behandling.resultat) {
            is Omgjøringsresultat.OmgjøringInnvilgelse,
            is Revurderingsresultat.Innvilgelse,
            is Søknadsbehandlingsresultat.Innvilgelse,
            -> this.beregnInnvilgelse(
                behandlingId = behandlingId,
                vedtaksperiode = behandling.innvilgelsesperioder!!.totalPeriode,
                innvilgelsesperioder = behandling.innvilgelsesperioder!!,
                barnetilleggsperioder = behandling.barnetillegg!!.periodisering,
            )

            is Omgjøringsresultat.OmgjøringOpphør -> this.beregnOpphør(
                behandlingId = behandlingId,
                opphørsperiode = vedtaksperiode,
            )

            is Revurderingsresultat.Stans -> this.beregnRevurderingStans(
                behandlingId = behandlingId,
                stansperiode = vedtaksperiode,
            )

            is Søknadsbehandlingsresultat.Avslag,
            is Omgjøringsresultat.OmgjøringIkkeValgt,
            null,
            -> null
        }?.let { beregning ->
            // abn: Vurder å hente nav kontor på nytt her...
            val navkontor =
                behandling.utbetaling?.navkontor ?: this.meldekortbehandlinger.hentSisteMeldekortBehandlingForKjede(
                    beregning.last().kjedeId,
                )?.navkontor

            if (navkontor == null) {
                logger.error { "Kunne ikke finne navkontor for behandling ${behandling.id} - kan ikke simulere" }
                return KanIkkeSendeRammebehandlingTilBeslutter.KunneIkkeHenteNavkontorForUtbetaling.left()
            }

            val simuleringMedMetadata = simulerService.simulerSøknadsbehandlingEllerRevurdering(
                behandling = behandling,
                beregning = beregning,
                forrigeUtbetaling = this.utbetalinger.lastOrNull(),
                meldeperiodeKjeder = this.meldeperiodeKjeder,
                saksbehandler = behandling.saksbehandler!!,
                kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
                brukersNavkontor = { navkontor },
            ).getOrElse {
                return KanIkkeSendeRammebehandlingTilBeslutter.SimuleringFeilet(it).left()
            }

            BehandlingUtbetaling(
                beregning = beregning,
                navkontor = navkontor,
                simulering = simuleringMedMetadata.simulering,
            ) to simuleringMedMetadata
        }.right()
    }
}
