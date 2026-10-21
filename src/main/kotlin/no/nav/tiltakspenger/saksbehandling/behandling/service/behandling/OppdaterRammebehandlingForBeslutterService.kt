package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter.KanIkkeOppdatereRammebehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter.OppdaterRammebehandlingForBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdaterForBeslutter.oppdaterForBeslutter
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OppdaterRammebehandlingForBeslutterService(
    private val behandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
) {
    fun oppdaterForBeslutter(
        kommando: OppdaterRammebehandlingForBeslutterKommando,
    ): Either<KanIkkeOppdatereRammebehandlingForBeslutter, Pair<Sak, Rammebehandling>> {
        val (sak, rammebehandling) = behandlingService.hentSakOgRammebehandling(kommando.sakId, kommando.behandlingId)

        return rammebehandling.oppdaterForBeslutter(kommando, clock).map { oppdatertRammebehandling ->
            val vellykket = rammebehandlingRepo.lagreHvisFortsattUnderBeslutning(
                rammebehandling = oppdatertRammebehandling,
                utøvendeBeslutter = kommando.beslutter,
                transactionContext = null,
            )

            if (!vellykket) return KanIkkeOppdatereRammebehandlingForBeslutter.BehandlingenErIkkeLengerUnderBeslutning.left()

            sak.oppdaterRammebehandling(oppdatertRammebehandling) to oppdatertRammebehandling
        }
    }
}
