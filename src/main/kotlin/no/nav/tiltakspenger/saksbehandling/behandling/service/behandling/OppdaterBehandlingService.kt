package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.validerStansDato
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
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

    suspend fun oppdater(kommando: OppdaterBehandlingKommando): Either<KanIkkeOppdatereBehandling, Pair<Sak, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val behandling: Rammebehandling = sak.hentBehandling(kommando.behandlingId)!!

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return BehandlingenEiesAvAnnenSaksbehandler(behandling.saksbehandler).left()
        }
        val (utbetaling, simuleringMedMetadata) = sak.beregnOgSimulerHvisAktuelt(kommando, behandling)

        return when (kommando) {
            is OppdaterSøknadsbehandlingKommando -> sak.oppdaterSøknadsbehandling(kommando, utbetaling)
            is OppdaterRevurderingKommando -> sak.oppdaterRevurdering(kommando, utbetaling)
        }.map { oppdatertBehandling: Rammebehandling ->
            val oppdatertSak = sak.oppdaterBehandling(oppdatertBehandling)

            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(oppdatertBehandling, tx)
                behandlingRepo.oppdaterSimuleringMetadata(
                    oppdatertBehandling.id,
                    simuleringMedMetadata?.originalResponseBody,
                    tx,
                )
            }
            oppdatertSak to oppdatertBehandling
        }
    }

    suspend fun Sak.beregnOgSimulerHvisAktuelt(
        kommando: OppdaterBehandlingKommando,
        behandling: Rammebehandling,
    ): Pair<BehandlingUtbetaling?, SimuleringMedMetadata?> {
        val beregning = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Innvilgelse,
            is OppdaterRevurderingKommando.Innvilgelse,
            -> this.beregnInnvilgelse(
                behandlingId = kommando.behandlingId,
                virkningsperiode = kommando.innvilgelsesperiode,
                barnetillegg = kommando.barnetillegg,
            )

            is OppdaterRevurderingKommando.Stans -> this.beregnRevurderingStans(
                behandlingId = kommando.behandlingId,
                stansFraOgMed = kommando.stansFraOgMed,
            )

            is OppdaterSøknadsbehandlingKommando.Avslag,
            is OppdaterSøknadsbehandlingKommando.IkkeValgtResultat,
            -> null
        }

        return beregning?.let {
            val navkontor = navkontorService.hentOppfolgingsenhet(this.fnr)
            val simuleringMedMetadata = simulerService.simulerSøknadsbehandlingEllerRevurdering(
                // Merk at behandlingen vi sender inn her er som den kom fra basen. Kanskje vi heller bare skal sende inn od, sakId, fnr og saksnummer?
                behandling = behandling,
                beregning = it,
                forrigeUtbetaling = this.utbetalinger.lastOrNull(),
                meldeperiodeKjeder = this.meldeperiodeKjeder,
                saksbehandler = kommando.saksbehandler.navIdent,
            ) { navkontor }.getOrElse { null }

            BehandlingUtbetaling(
                beregning = it,
                navkontor = navkontor,
                simulering = simuleringMedMetadata?.simulering,
            ) to simuleringMedMetadata
        } ?: (null to null)
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
            is OppdaterRevurderingKommando.Innvilgelse -> {
                revurdering.oppdaterInnvilgelse(
                    kommando = kommando,
                    utbetaling = utbetaling,
                    clock = clock,
                )
            }

            is OppdaterRevurderingKommando.Stans -> {
                validerStansDato(kommando.stansFraOgMed)

                revurdering.oppdaterStans(
                    kommando = kommando,
                    sisteDagSomGirRett = sisteDagSomGirRett!!,
                    clock = clock,
                    utbetaling = utbetaling,
                )
            }
        }
    }
}
