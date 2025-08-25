package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerStansDato
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

class OppdaterBehandlingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val navkontorService: NavkontorService,
    private val clock: Clock,
    private val simulerService: SimulerService,
    private val sessionFactory: SessionFactory,
) {

    suspend fun oppdater(kommando: OppdaterBehandlingKommando): Either<KanIkkeOppdatereBehandling, Pair<Sak, Behandling>> {
        val sak = sakService.hentForSakId(
            sakId = kommando.sakId,
        )

        val behandling = sak.hentBehandling(kommando.behandlingId)

        requireNotNull(behandling) {
            "Fant ikke behandlingen ${kommando.behandlingId} på sak ${kommando.sakId}"
        }

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
                .left()
        }

        return when (kommando) {
            is OppdaterSøknadsbehandlingKommando -> sak.oppdaterSøknadsbehandling(kommando).map { it to null }
            is OppdaterRevurderingKommando -> sak.oppdaterRevurdering(kommando)
        }.map { (behandling, simulering) ->
            val oppdatertSak = sak.oppdaterBehandling(behandling)

            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(behandling, tx)
                behandlingRepo.oppdaterSimuleringMetadata(behandling.id, simulering?.originalResponseBody, tx)
            }
            oppdatertSak to behandling
        }
    }

    private fun Sak.oppdaterSøknadsbehandling(
        kommando: OppdaterSøknadsbehandlingKommando,
    ): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
        kommando.asInnvilgelseOrNull()?.takeIf {
            this.utbetalinger.hentUtbetalingerFraPeriode(it.innvilgelsesperiode).isNotEmpty()
        }?.let {
            return KanIkkeOppdatereBehandling.InnvilgelsesperiodenOverlapperMedUtbetaltPeriode.left()
        }

        val behandling = this.hentBehandling(kommando.behandlingId) as Søknadsbehandling

        return behandling.oppdater(kommando, clock)
    }

    private suspend fun Sak.oppdaterRevurdering(
        kommando: OppdaterRevurderingKommando,
    ): Either<KanIkkeOppdatereBehandling, Pair<Revurdering, SimuleringMedMetadata?>> {
        val behandling = this.hentBehandling(kommando.behandlingId) as Revurdering

        return when (kommando) {
            is Innvilgelse -> {
                val (utbetaling, simuleringMedMetadata) = beregnRevurderingInnvilgelse(kommando)?.let {
                    val navkontor = navkontorService.hentOppfolgingsenhet(this.fnr)
                    val simuleringMedMetadata = simulerService.simulerRevurdering(
                        behandling = behandling,
                        beregning = it,
                        forrigeUtbetaling = this.utbetalinger.lastOrNull(),
                        meldeperiodeKjeder = this.meldeperiodeKjeder,
                    ) { navkontor }.getOrElse { null }

                    BehandlingUtbetaling(
                        beregning = it,
                        navkontor = navkontor,
                        simulering = simuleringMedMetadata?.simulering,
                    ) to simuleringMedMetadata
                } ?: (null to null)
                behandling.oppdaterInnvilgelse(
                    kommando = kommando,
                    utbetaling = utbetaling,
                    clock = clock,
                ).map { it to simuleringMedMetadata }
            }

            is Stans -> {
                validerStansDato(kommando.stansFraOgMed)

                behandling.oppdaterStans(
                    kommando = kommando,
                    sisteDagSomGirRett = sisteDagSomGirRett!!,
                    clock = clock,
                ).map { it to null }
            }
        }
    }
}
