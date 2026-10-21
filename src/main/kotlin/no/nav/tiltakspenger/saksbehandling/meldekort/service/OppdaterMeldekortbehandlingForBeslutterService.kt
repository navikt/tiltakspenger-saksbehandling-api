package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter.KanIkkeOppdatereMeldekortbehandlingForBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter.OppdaterMeldekortbehandlingForBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdaterForBeslutter.oppdaterForBeslutter
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OppdaterMeldekortbehandlingForBeslutterService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val clock: Clock,
    private val sakService: SakService,
) {
    fun oppdaterForBeslutter(
        kommando: OppdaterMeldekortbehandlingForBeslutterKommando,
    ): Either<KanIkkeOppdatereMeldekortbehandlingForBeslutter, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)
        requireNotNull(meldekortbehandling) {
            "Fant ikke meldekortbehandling ${kommando.meldekortId} på sak ${kommando.sakId}"
        }

        if (meldekortbehandling !is MeldekortbehandlingManuell) {
            return KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErIkkeUnderBeslutning.left()
        }

        return meldekortbehandling.oppdaterForBeslutter(kommando, clock).map { oppdatertMeldekortbehandling ->
            if (!meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(oppdatertMeldekortbehandling, utøvendeBeslutter = kommando.saksbehandler)) {
                return KanIkkeOppdatereMeldekortbehandlingForBeslutter.BehandlingenErIkkeLengerUnderBeslutning.left()
            }
            sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling) to oppdatertMeldekortbehandling
        }
    }
}
