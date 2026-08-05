package no.nav.tiltakspenger.saksbehandling.behandling.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOpphør
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import java.time.LocalDateTime

class OppdaterBeregningOgSimuleringRammebehandlingService(
    val sakService: SakService,
    val rammebehandlingRepo: RammebehandlingRepo,
    val simulerService: SimulerService,
    val sessionFactory: SessionFactory,
    val clock: Clock,
) {
    /**
     * Oppdaterer beregning og simuleringen av utbetaling på en åpen rammebehandling som er under behandling eller beslutning.
     */
    suspend fun oppdaterSimulering(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Rammebehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val behandling = sak.hentRammebehandling(behandlingId)!!

        val beregningOgSimulering = sak.beregnOgSimuler(
            behandling = behandling,
            saksbehandlerEllerBeslutter = saksbehandler,
        ).getOrElse { return it.left() }

        val oppdatertUtbetaling = beregningOgSimulering?.let {
            BehandlingUtbetaling(
                beregning = it.first,
                simulering = it.second.simulering,
                navkontor = sak.behandlinger.sisteNavkontor!!,
            )
        }

        val oppdatertBehandling = behandling.oppdaterUtbetaling(
            oppdatertUtbetaling = oppdatertUtbetaling,
            clock = clock,
        )
        val oppdatertSak = sak.oppdaterRammebehandling(oppdatertBehandling)

        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(oppdatertBehandling, tx)
            rammebehandlingRepo.oppdaterSimuleringMetadata(
                oppdatertBehandling.id,
                beregningOgSimulering?.second?.originalResponseBody,
                tx,
            )
        }

        return (oppdatertSak to oppdatertBehandling).right()
    }

    /**
     * Beregner og simulerer rammebehandlingen på nytt slik saken ser ut nå, og setter resultatet som [Utbetalingskontroll] på behandlingen.
     * Kjøres når behandlingen sendes videre i flyten, altså til beslutter og ved iverksettelse.
     *
     * Behandlingen persisteres ikke her; det er kallerens ansvar.
     */
    suspend fun oppdaterUtbetalingskontroll(
        sak: Sak,
        behandlingId: RammebehandlingId,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Rammebehandling>> {
        val behandling = sak.hentRammebehandling(behandlingId)!!

        val beregningOgSimulering = sak.beregnOgSimuler(
            behandling = behandling,
            saksbehandlerEllerBeslutter = saksbehandlerEllerBeslutter,
        ).getOrElse { return it.left() }

        val utbetalingskontroll: Utbetalingskontroll? = beregningOgSimulering?.let {
            Utbetalingskontroll(
                beregning = it.first,
                simulering = it.second.simulering,
            )
        }

        val oppdatertBehandling = behandling.oppdaterUtbetalingskontroll(
            oppdatertKontroll = utbetalingskontroll,
            clock = clock,
        )
        val oppdatertSak = sak.oppdaterRammebehandling(oppdatertBehandling)

        return (oppdatertSak to oppdatertBehandling).right()
    }

    private suspend fun Sak.beregnOgSimuler(
        behandling: Rammebehandling,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Beregning, SimuleringMedMetadata>?> {
        val nå = nå(clock)
        if (behandling.erUnderBehandling) {
            require(saksbehandlerEllerBeslutter.navIdent == behandling.saksbehandler) {
                "Kan kun oppdatere simulering på en behandling dersom saksbehandler som ber om det er den samme som er satt på behandlingen"
            }
        } else if (behandling.erUnderBeslutning) {
            require(saksbehandlerEllerBeslutter.navIdent == behandling.beslutter) {
                "Kan kun oppdatere simulering på en behandling dersom beslutter som ber om det er den samme som er satt på behandlingen"
            }
        } else {
            throw IllegalStateException("Rammebehandling må være under behandling eller beslutning for at simulering skal kunne oppdateres")
        }

        val beregning = this.beregn(behandling, nå) ?: return null.right()

        val simulering: SimuleringMedMetadata =
            beregning.let { beregning ->
                val navkontor = this.behandlinger.sisteNavkontor!!

                simulerService.simulerSøknadsbehandlingEllerRevurdering(
                    behandling = behandling,
                    beregning = beregning,
                    forrigeUtbetaling = this.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = this.meldeperiodeKjeder,
                    saksbehandler = saksbehandlerEllerBeslutter.navIdent,
                    brukersNavkontor = { navkontor },
                    kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
                ).getOrElse { return it.left() }
            }

        return (beregning to simulering).right()
    }

    private fun Sak.beregn(
        behandling: Rammebehandling,
        beregningstidspunkt: LocalDateTime,
    ): Beregning? {
        val behandlingId = behandling.id

        fun feilmelding(felt: String): String =
            "$felt kan ikke være null ved beregning." +
                " sakId: ${behandling.sakId}, saksnummer: ${behandling.saksnummer}, behandlingId: $behandlingId," +
                " status: ${behandling.status}, type: ${behandling.behandlingstype}," +
                " resultat: ${behandling.resultat?.let { it::class.simpleName }}"

        return when (behandling.resultat) {
            is Omgjøringsresultat.OmgjøringInnvilgelse,
            is Revurderingsresultat.Innvilgelse,
            is Søknadsbehandlingsresultat.Innvilgelse,
            -> this.beregnInnvilgelse(
                behandlingId = behandlingId,
                vedtaksperiode = behandling.vedtaksperiode ?: error(feilmelding("vedtaksperiode")),
                innvilgelsesperioder = behandling.innvilgelsesperioder ?: error(feilmelding("innvilgelsesperioder")),
                barnetilleggsperioder = (behandling.barnetillegg ?: error(feilmelding("barnetillegg"))).periodisering,
                beregningstidspunkt = beregningstidspunkt,
            )

            is Omgjøringsresultat.OmgjøringOpphør -> this.beregnOpphør(
                behandlingId = behandlingId,
                opphørsperiode = behandling.vedtaksperiode ?: error(feilmelding("vedtaksperiode")),
                beregningstidspunkt = beregningstidspunkt,
            )

            is Revurderingsresultat.Stans -> this.beregnRevurderingStans(
                behandlingId = behandlingId,
                stansperiode = behandling.vedtaksperiode ?: error(feilmelding("vedtaksperiode")),
                beregningstidspunkt = beregningstidspunkt,
            )

            is Søknadsbehandlingsresultat.Avslag,
            is Omgjøringsresultat.OmgjøringIkkeValgt,
            null,
            -> null
        }
    }
}
