package no.nav.tiltakspenger.saksbehandling.meldekort.service.overta

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OvertaMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
) {
    fun overta(
        command: OvertaMeldekortBehandlingCommand,
    ): Either<KunneIkkeOvertaMeldekortBehandling, Pair<Sak, MeldekortBehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortBehandling: MeldekortBehandling = sak.hentMeldekortBehandling(command.meldekortId)!!

        return meldekortBehandling.overta(command.saksbehandler).map {
            when (it.status) {
                MeldekortBehandlingStatus.UNDER_BEHANDLING -> meldekortBehandlingRepo.overtaSaksbehandler(
                    meldekortId = it.id,
                    nySaksbehandler = command.saksbehandler,
                    nåværendeSaksbehandler = command.overtarFra,
                )

                MeldekortBehandlingStatus.UNDER_BESLUTNING -> meldekortBehandlingRepo.overtaBeslutter(
                    meldekortId = it.id,
                    nyBeslutter = command.saksbehandler,
                    nåværendeBeslutter = command.overtarFra,
                )

                else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne overta")
            }.also { harOvertatt ->
                require(harOvertatt) {
                    "Oppdatering av saksbehandler i db feilet ved overta meldekortbehandling for ${command.meldekortId}"
                }
            }
            (sak.oppdaterMeldekortbehandling(it) to it)
        }
    }
}
