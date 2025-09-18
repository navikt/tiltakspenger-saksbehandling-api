package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeAvbryteMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.AvbrytMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDateTime

class AvbrytMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
) {
    fun avbryt(command: AvbrytMeldekortBehandlingCommand): Either<KanIkkeAvbryteMeldekortBehandling, Pair<Sak, MeldekortBehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortBehandling = sak.hentMeldekortBehandling(command.meldekortId)

        if (meldekortBehandling !is MeldekortUnderBehandling) {
            return KanIkkeAvbryteMeldekortBehandling.MåVæreUnderBehandling.left()
        }
        return meldekortBehandling.avbryt(command.saksbehandler, command.begrunnelse, LocalDateTime.now()).map {
            when (it.status) {
                MeldekortBehandlingStatus.AVBRUTT -> meldekortBehandlingRepo.oppdater(it)
                else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne avbryte")
            }
            Pair(sak.oppdaterMeldekortbehandling(it), it)
        }
    }
}
