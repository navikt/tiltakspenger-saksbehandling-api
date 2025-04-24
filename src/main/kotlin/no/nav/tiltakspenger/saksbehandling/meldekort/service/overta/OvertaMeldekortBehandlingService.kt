package no.nav.tiltakspenger.saksbehandling.meldekort.service.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo

class OvertaMeldekortBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
) {
    suspend fun overta(command: OvertaMeldekortBehandlingCommand): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        val meldekortBehandling = meldekortBehandlingRepo.hent(command.meldekortId)
            ?: throw IllegalStateException("Fant ikke meldekortBehandling for id ${command.meldekortId}")
        tilgangsstyringService.harTilgangTilPerson(
            meldekortBehandling.fnr,
            command.saksbehandler.roller,
            command.correlationId,
        )
            .onLeft {
                throw TilgangException("Feil ved tilgangssjekk til person ved overtakelse av meldekortbehandling. Feilen var $it")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${command.saksbehandler.navIdent} har ikke tilgang til person")
            }

        return meldekortBehandling.overta(command.saksbehandler).onRight {
            when (it.status) {
                MeldekortBehandlingStatus.UNDER_BEHANDLING -> meldekortBehandlingRepo.overtaSaksbehandler(
                    it.id,
                    command.saksbehandler,
                    command.overtarFra,
                )

                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                MeldekortBehandlingStatus.GODKJENT,
                MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
                -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne overta")
            }
        }
    }
}
