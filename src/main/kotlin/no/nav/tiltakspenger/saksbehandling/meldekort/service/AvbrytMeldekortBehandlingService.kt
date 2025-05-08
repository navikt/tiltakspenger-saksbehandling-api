package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeAvbryteMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.AvbrytMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import java.time.LocalDateTime

class AvbrytMeldekortBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
) {
    suspend fun avbryt(command: AvbrytMeldekortBehandlingCommand): Either<KanIkkeAvbryteMeldekortBehandling, MeldekortBehandling> {
        val meldekortBehandling = meldekortBehandlingRepo.hent(command.meldekortId)
            ?: throw IllegalStateException("Fant ikke meldekortBehandling for id ${command.meldekortId}")
        tilgangsstyringService.harTilgangTilPerson(
            meldekortBehandling.fnr,
            command.saksbehandler.roller,
            command.correlationId,
        )
            .onLeft {
                throw TilgangException("Feil ved tilgangssjekk til person ved avbryting av meldekortbehandling. Feilen var $it")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${command.saksbehandler.navIdent} har ikke tilgang til person")
            }
        if (meldekortBehandling is MeldekortUnderBehandling) {
            return meldekortBehandling.avbryt(command.saksbehandler, command.begrunnelse, LocalDateTime.now()).onRight {
                when (it.status) {
                    MeldekortBehandlingStatus.AVBRUTT -> meldekortBehandlingRepo.oppdater(it)
                    MeldekortBehandlingStatus.UNDER_BEHANDLING,
                    MeldekortBehandlingStatus.UNDER_BESLUTNING,
                    MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                    MeldekortBehandlingStatus.GODKJENT,
                    MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                    MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
                    -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne overta")
                }
            }
        } else {
            return KanIkkeAvbryteMeldekortBehandling.MåVæreUnderBehandling.left()
        }
    }
}
