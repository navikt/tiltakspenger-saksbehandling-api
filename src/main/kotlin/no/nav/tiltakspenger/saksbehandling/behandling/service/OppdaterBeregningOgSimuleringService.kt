package no.nav.tiltakspenger.saksbehandling.behandling.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOpphør
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

class OppdaterBeregningOgSimuleringService(
    val sakService: SakService,
    val rammebehandlingRepo: RammebehandlingRepo,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val simulerService: SimulerService,
    val sessionFactory: SessionFactory,
    val clock: Clock,
) {
    /**
     * Oppdaterer beregning og simuleringen av utbetaling på en åpen behandling som er under behandling eller beslutning
     * @param behandlingId id til behandlingen som skal oppdateres ([BehandlingId] eller [MeldekortId])
     */
    suspend fun oppdaterSimulering(
        sakId: SakId,
        behandlingId: Ulid,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Either<Rammebehandling, MeldekortBehandling>>> {
        val sak: Sak = sakService.hentForSakId(sakId)

        return if (behandlingId.erBehandlingId()) {
            sak.oppdaterRammebehandling(
                behandlingId = behandlingId.toBehandlingId(),
                saksbehandlerEllerBeslutter = saksbehandler,
            ).map { (oppdatertSak, oppdatertBehandling) ->
                (oppdatertSak to oppdatertBehandling.left())
            }
        } else {
            sak.oppdaterMeldekortbehandling(
                meldekortbehandlingId = behandlingId.toMeldekortId(),
                saksbehandler = saksbehandler,
            )
        }
    }

    suspend fun oppdaterUtbetalingskontroll(
        sak: Sak,
        behandlingId: BehandlingId,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Rammebehandling>> {
        val behandling = sak.hentRammebehandling(behandlingId)!!

        val beregningOgSimulering = sak.beregnOgSimulerRammebehandling(
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

    private suspend fun Sak.oppdaterRammebehandling(
        behandlingId: BehandlingId,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Rammebehandling>> {
        val behandling = this.hentRammebehandling(behandlingId)!!

        val beregningOgSimulering = this.beregnOgSimulerRammebehandling(
            behandling = behandling,
            saksbehandlerEllerBeslutter = saksbehandlerEllerBeslutter,
        ).getOrElse { return it.left() }

        val oppdatertUtbetaling = beregningOgSimulering?.let {
            BehandlingUtbetaling(
                beregning = it.first,
                simulering = it.second.simulering,
                navkontor = this.behandlinger.sisteNavkontor!!,
            )
        }

        val oppdatertBehandling = behandling.oppdaterUtbetaling(
            oppdatertUtbetaling = oppdatertUtbetaling,
            clock = clock,
        )
        val oppdatertSak = this.oppdaterRammebehandling(oppdatertBehandling)

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

    private suspend fun Sak.beregnOgSimulerRammebehandling(
        behandling: Rammebehandling,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Beregning, SimuleringMedMetadata>?> {
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

        val beregning = this.beregnRammebehandling(behandling) ?: return null.right()

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

    private suspend fun Sak.oppdaterMeldekortbehandling(
        meldekortbehandlingId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Either<Rammebehandling, MeldekortBehandling>>> {
        val meldekortbehandling: MeldekortBehandling = this.hentMeldekortBehandling(meldekortbehandlingId)!!

        require(saksbehandler.navIdent == meldekortbehandling.saksbehandler) {
            "Kan kun oppdatere simulering på en behandling dersom saksbehandler som ber om det er den samme som er satt på behandlingen"
        }

        val simuleringMedMetadata: SimuleringMedMetadata = simulerService.simulerMeldekort(
            behandling = meldekortbehandling,
            forrigeUtbetaling = this.utbetalinger.lastOrNull(),
            meldeperiodeKjeder = this.meldeperiodeKjeder,
            brukersNavkontor = { meldekortbehandling.navkontor },
            kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
        ).getOrElse {
            return it.left()
        }

        val oppdatertMeldekortbehandling = meldekortbehandling.oppdaterSimulering(simuleringMedMetadata.simulering)
        val oppdatertSak = this.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling)
        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.oppdater(oppdatertMeldekortbehandling, simuleringMedMetadata, tx)
        }
        return (oppdatertSak to oppdatertMeldekortbehandling.right()).right()
    }

    private fun Sak.beregnRammebehandling(behandling: Rammebehandling): Beregning? {
        val behandlingId = behandling.id
        val vedtaksperiode = behandling.vedtaksperiode!!

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
        }
    }
}

private fun Ulid.toBehandlingId(): BehandlingId = BehandlingId.fromString(this.toString())
private fun Ulid.toMeldekortId(): MeldekortId = MeldekortId.fromString(this.toString())

private fun Ulid.erBehandlingId(): Boolean = Either.catch { BehandlingId.fromString(this.toString()) }.fold(
    ifLeft = { false },
    ifRight = { true },
)
