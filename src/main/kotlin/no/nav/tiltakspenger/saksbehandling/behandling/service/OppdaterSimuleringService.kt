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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService

class OppdaterSimuleringService(
    val sakService: SakService,
    val rammebehandlingRepo: RammebehandlingRepo,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val simulerService: SimulerService,
    val sessionFactory: SessionFactory,
) {
    /**
     * Oppdaterer simuleringen på en åpen behandling som ikke er sendt til beslutter.
     * @param behandlingId id til behandlingen som skal oppdateres ([BehandlingId] eller [MeldekortId])
     */
    suspend fun oppdaterSimulering(
        sakId: SakId,
        behandlingId: Ulid,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Either<Rammebehandling, MeldekortBehandling>>> {
        val sak: Sak = sakService.hentForSakId(sakId)

        val simuler: suspend (beregning: Beregning, navkontor: Navkontor) -> Either<KunneIkkeSimulere, SimuleringMedMetadata> =
            { beregning, navkontor ->
                simulerService.simuler(
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    behandlingId = behandlingId,
                    fnr = sak.fnr,
                    beregning = beregning,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = sak.meldeperiodeKjeder,
                    saksbehandler = saksbehandler.navIdent,
                    brukersNavkontor = { navkontor },
                    kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
                )
            }
        return if (behandlingId.erBehandlingId()) {
            sak.oppdaterRammebehandling(
                behandlingId = behandlingId.toBehandlingId(),
                saksbehandler = saksbehandler,
                simuler = simuler,
            )
        } else {
            sak.oppdaterMeldekortbehandling(
                meldekortbehandlingId = behandlingId.toMeldekortId(),
                saksbehandler = saksbehandler,
                simuler = simuler,
            )
        }
    }

    private suspend fun Sak.oppdaterRammebehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        simuler: suspend (beregning: Beregning, navkontor: Navkontor) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
    ): Either<KunneIkkeSimulere, Pair<Sak, Either<Rammebehandling, MeldekortBehandling>>> {
        val behandling: Rammebehandling = this.hentRammebehandling(behandlingId)!!
        require(saksbehandler.navIdent == behandling.saksbehandler) {
            "Kan kun oppdatere simulering på en behandling dersom saksbehandler som ber om det er den samme som er satt på behandlingen"
        }
        val simuleringMedMetadata: SimuleringMedMetadata =
            simuler(behandling.utbetaling!!.beregning, behandling.utbetaling!!.navkontor).getOrElse {
                return it.left()
            }
        val oppdatertBehandling = behandling.oppdaterSimulering(simuleringMedMetadata.simulering)
        val oppdatertSak = this.oppdaterRammebehandling(oppdatertBehandling)
        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(oppdatertBehandling, tx)
            rammebehandlingRepo.oppdaterSimuleringMetadata(behandlingId, simuleringMedMetadata.originalResponseBody, tx)
        }
        return (oppdatertSak to oppdatertBehandling.left()).right()
    }

    private suspend fun Sak.oppdaterMeldekortbehandling(
        meldekortbehandlingId: MeldekortId,
        saksbehandler: Saksbehandler,
        simuler: suspend (beregning: Beregning, navkontor: Navkontor) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
    ): Either<KunneIkkeSimulere, Pair<Sak, Either<Rammebehandling, MeldekortBehandling>>> {
        val meldekortbehandling: MeldekortBehandling = this.hentMeldekortBehandling(meldekortbehandlingId)!!
        require(saksbehandler.navIdent == meldekortbehandling.saksbehandler) {
            "Kan kun oppdatere simulering på en behandling dersom saksbehandler som ber om det er den samme som er satt på behandlingen"
        }
        val simuleringMedMetadata: SimuleringMedMetadata =
            simuler(meldekortbehandling.beregning!!, meldekortbehandling.navkontor).getOrElse {
                return it.left()
            }
        val oppdatertMeldekortbehandling = meldekortbehandling.oppdaterSimulering(simuleringMedMetadata.simulering)
        val oppdatertSak = this.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling)
        sessionFactory.withTransactionContext { tx ->
            meldekortBehandlingRepo.oppdater(oppdatertMeldekortbehandling, simuleringMedMetadata, tx)
        }
        return (oppdatertSak to oppdatertMeldekortbehandling.right()).right()
    }
}

private fun Ulid.toBehandlingId(): BehandlingId = BehandlingId.fromString(this.toString())
private fun Ulid.toMeldekortId(): MeldekortId = MeldekortId.fromString(this.toString())

private fun Ulid.erBehandlingId(): Boolean = Either.catch { BehandlingId.fromString(this.toString()) }.fold(
    ifLeft = { false },
    ifRight = { true },
)
