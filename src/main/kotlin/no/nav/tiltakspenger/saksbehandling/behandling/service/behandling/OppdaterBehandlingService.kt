package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler
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
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
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
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val behandling: Revurdering = sak.hentBehandling(kommando.behandlingId) as Revurdering

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return BehandlingenEiesAvAnnenSaksbehandler(behandling.saksbehandler).left()
        }
        val (utbetaling, simuleringMedMetadata) = sak.beregnInnvilgelse(kommando)?.let {
            val navkontor = navkontorService.hentOppfolgingsenhet(sak.fnr)
            val simuleringMedMetadata = simulerService.simulerRevurdering(
                behandling = behandling,
                beregning = it,
                forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                meldeperiodeKjeder = sak.meldeperiodeKjeder,
            ) { navkontor }.getOrElse { null }

            BehandlingUtbetaling(
                beregning = it,
                navkontor = navkontor,
                simulering = simuleringMedMetadata?.simulering,
            ) to simuleringMedMetadata
        } ?: (null to null)

        return when (kommando) {
            is OppdaterSøknadsbehandlingKommando -> sak.oppdaterSøknadsbehandling(kommando, utbetaling)
                .map { it to null }

            is OppdaterRevurderingKommando -> sak.oppdaterRevurdering(kommando, utbetaling)
        }.map {
            val oppdatertSak = sak.oppdaterBehandling(behandling)

            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(behandling, tx)
                behandlingRepo.oppdaterSimuleringMetadata(
                    behandling.id,
                    simuleringMedMetadata?.originalResponseBody,
                    tx,
                )
            }
            oppdatertSak to behandling
        }
    }

    private fun Sak.oppdaterSøknadsbehandling(
        kommando: OppdaterSøknadsbehandlingKommando,
        utbetaling: BehandlingUtbetaling?,
    ): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
        val søknadsbehandling: Søknadsbehandling = this.hentBehandling(kommando.behandlingId) as Søknadsbehandling

        return søknadsbehandling.oppdater(kommando, clock, utbetaling)
    }

    private fun Sak.oppdaterRevurdering(
        kommando: OppdaterRevurderingKommando,
        utbetaling: BehandlingUtbetaling?,
    ): Either<KanIkkeOppdatereBehandling, Revurdering> {
        val revurdering: Revurdering = this.hentBehandling(kommando.behandlingId) as Revurdering

        return when (kommando) {
            is Innvilgelse -> {
                revurdering.oppdaterInnvilgelse(
                    kommando = kommando,
                    utbetaling = utbetaling,
                    clock = clock,
                )
            }

            is Stans -> {
                validerStansDato(kommando.stansFraOgMed)

                revurdering.oppdaterStans(
                    kommando = kommando,
                    sisteDagSomGirRett = sisteDagSomGirRett!!,
                    clock = clock,
                )
            }
        }
    }
}
