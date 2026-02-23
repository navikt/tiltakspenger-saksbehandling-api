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
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.beregning.beregnOpphør
import no.nav.tiltakspenger.saksbehandling.beregning.beregnRevurderingStans
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService

class OppdaterBeregningOgSimuleringService(
    val sakService: SakService,
    val rammebehandlingRepo: RammebehandlingRepo,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val simulerService: SimulerService,
    val sessionFactory: SessionFactory,
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
            val behandling = sak.hentRammebehandling(behandlingId.toBehandlingId())!!

            hentOppdatertBeregningOgSimuleringForRammebehandling(
                sak = sak,
                behandling = behandling,
                saksbehandlerEllerBeslutter = saksbehandler,
            ).map { (oppdatertSak, oppdatertBehandling, oppdatertSimulering) ->
                lagreRammebehandling(oppdatertBehandling, oppdatertSimulering)
                (oppdatertSak to oppdatertBehandling.left())
            }
        } else {
            sak.oppdaterMeldekortbehandling(
                meldekortbehandlingId = behandlingId.toMeldekortId(),
                saksbehandler = saksbehandler,
            )
        }
    }

    suspend fun hentOppdatertBeregningOgSimuleringForRammebehandling(
        sak: Sak,
        behandling: Rammebehandling,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Triple<Sak, Rammebehandling, SimuleringMedMetadata?>> {
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

        val utbetalingOgSimulering: Pair<BehandlingUtbetaling, SimuleringMedMetadata>? =
            sak.beregnRammebehandling(behandling)?.let { beregning ->
                val navkontor = sak.behandlinger.sisteNavkontor!!

                val simulering = simulerService.simulerSøknadsbehandlingEllerRevurdering(
                    behandling = behandling,
                    beregning = beregning,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = sak.meldeperiodeKjeder,
                    saksbehandler = saksbehandlerEllerBeslutter.navIdent,
                    brukersNavkontor = { navkontor },
                    kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
                ).getOrElse { return it.left() }

                BehandlingUtbetaling(
                    beregning = beregning,
                    navkontor = navkontor,
                    simulering = simulering.simulering,
                ) to simulering
            }

        val oppdatertBehandling = behandling.oppdaterUtbetaling(utbetalingOgSimulering?.first)
        val oppdatertSak = sak.oppdaterRammebehandling(oppdatertBehandling)

        return Triple(
            oppdatertSak,
            oppdatertBehandling,
            utbetalingOgSimulering?.second,
        ).right()
    }

    private fun lagreRammebehandling(behandling: Rammebehandling, simulering: SimuleringMedMetadata?) {
        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(behandling, tx)
            rammebehandlingRepo.oppdaterSimuleringMetadata(
                behandling.id,
                simulering?.originalResponseBody,
                tx,
            )
        }
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
