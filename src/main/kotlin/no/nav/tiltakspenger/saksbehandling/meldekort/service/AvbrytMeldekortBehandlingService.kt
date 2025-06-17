package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
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
        val meldekortBehandling = meldekortBehandlingRepo.hent(command.meldekortId)!!

        tilgangsstyringService.krevTilgangTilPerson(
            command.saksbehandler,
            meldekortBehandling.fnr,
            command.correlationId,
        )

        if (meldekortBehandling is MeldekortUnderBehandling) {
            return meldekortBehandling.avbryt(command.saksbehandler, command.begrunnelse, LocalDateTime.now()).onRight {
                when (it.status) {
                    MeldekortBehandlingStatus.AVBRUTT -> meldekortBehandlingRepo.oppdater(it)
                    MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING,
                    MeldekortBehandlingStatus.UNDER_BEHANDLING,
                    MeldekortBehandlingStatus.UNDER_BESLUTNING,
                    MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                    MeldekortBehandlingStatus.GODKJENT,
                    MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                    MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
                    -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne avbryte")
                }
            }
        } else {
            return KanIkkeAvbryteMeldekortBehandling.MåVæreUnderBehandling.left()
        }
    }
}
