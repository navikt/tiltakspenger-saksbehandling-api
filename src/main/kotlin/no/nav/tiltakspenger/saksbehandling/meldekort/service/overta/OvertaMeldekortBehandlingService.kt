package no.nav.tiltakspenger.saksbehandling.meldekort.service.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo

class OvertaMeldekortBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
) {
    suspend fun overta(command: OvertaMeldekortBehandlingCommand): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        val meldekortBehandling = meldekortBehandlingRepo.hent(command.meldekortId)!!
        tilgangsstyringService.krevTilgangTilPerson(command.saksbehandler, meldekortBehandling.fnr, command.correlationId)

        return meldekortBehandling.overta(command.saksbehandler).onRight {
            when (it.status) {
                MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> meldekortBehandlingRepo.overtaSaksbehandler(
                    it.id,
                    command.saksbehandler,
                    command.overtarFra,
                )
                MeldekortBehandlingStatus.UNDER_BESLUTNING -> meldekortBehandlingRepo.overtaBeslutter(
                    it.id,
                    command.saksbehandler,
                    command.overtarFra,
                )
                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                MeldekortBehandlingStatus.GODKJENT,
                MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
                MeldekortBehandlingStatus.AVBRUTT,
                -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne overta")
            }
        }
    }
}
