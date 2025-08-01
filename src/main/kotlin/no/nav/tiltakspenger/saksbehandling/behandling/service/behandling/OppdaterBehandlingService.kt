package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse.Utbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerStansDato
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.RevurderingIkkeBeregnet
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OppdaterBehandlingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val navkontorService: NavkontorService,
    private val clock: Clock,
) {

    suspend fun oppdater(kommando: OppdaterBehandlingKommando): Either<KanIkkeOppdatereBehandling, Behandling> {
        krevSaksbehandlerRolle(kommando.saksbehandler)

        val sak = sakService.hentForSakId(
            sakId = kommando.sakId,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
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
            is OppdaterSøknadsbehandlingKommando -> sak.oppdaterSøknadsbehandling(kommando)
            is OppdaterRevurderingKommando -> sak.oppdaterRevurdering(kommando)
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

        return behandling.oppdater(kommando, clock).onRight {
            validerOgLagre(it)
        }
    }

    private suspend fun Sak.oppdaterRevurdering(
        kommando: OppdaterRevurderingKommando,
    ): Either<KanIkkeOppdatereBehandling, Revurdering> {
        val behandling = this.hentBehandling(kommando.behandlingId) as Revurdering

        return when (kommando) {
            is Innvilgelse -> {
                val utbetaling = beregnRevurderingInnvilgelse(kommando).fold(
                    ifLeft = {
                        when (it) {
                            is RevurderingIkkeBeregnet.IngenEndring -> null
                            is RevurderingIkkeBeregnet.StøtterIkkeTilbakekreving ->
                                return KanIkkeOppdatereBehandling.StøtterIkkeTilbakekreving.left()
                        }
                    },

                    ifRight = {
                        Utbetaling(
                            beregning = it,
                            navkontor = navkontorService.hentOppfolgingsenhet(this.fnr),
                        )
                    },
                )

                behandling.oppdaterInnvilgelse(
                    kommando = kommando,
                    utbetaling = utbetaling,
                    clock = clock,
                )
            }

            is Stans -> {
                validerStansDato(kommando.stansFraOgMed)

                behandling.oppdaterStans(
                    kommando = kommando.copy(sisteDagSomGirRett = sisteDagSomGirRett),
                    clock = clock,
                )
            }
        }.onRight {
            validerOgLagre(it)
        }
    }

    private fun Sak.validerOgLagre(behandling: Behandling) {
        this.copy(behandlinger = this.behandlinger.oppdaterBehandling(behandling))
        behandlingRepo.lagre(behandling)
    }
}
