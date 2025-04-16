package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KunneIkkeUnderkjenneMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UnderkjennMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import java.time.Clock

class UnderkjennMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val tilgangsstyringService: TilgangsstyringService,
    private val clock: Clock,
) {
    suspend fun underkjenn(command: UnderkjennMeldekortBehandlingCommand): Either<KunneIkkeUnderkjenneMeldekortBehandling, MeldekortBehandling> {
        val meldekortBehandling = meldekortBehandlingRepo.hent(command.meldekortId)
            ?: throw IllegalStateException("Fant ikke meldekortBehandling for id ${command.meldekortId}")

        if (meldekortBehandling !is MeldekortBehandletManuelt) {
            return KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeKlarTilBeslutning.left()
        }

        tilgangsstyringService.harTilgangTilPerson(meldekortBehandling.fnr, command.saksbehandler.roller, command.correlationId).onLeft {
            throw TilgangException("Feil ved tilgangssjekk til person ved sending av behandling tilbake til saksbehandler. Feilen var $it")
        }.onRight {
            if (!it) throw TilgangException("Saksbehandler ${command.saksbehandler.navIdent} har ikke tilgang til person")
        }

        val begrunnelse = Either.catch { command.begrunnelse.toNonBlankString() }.getOrElse {
            return KunneIkkeUnderkjenneMeldekortBehandling.BegrunnelseMåVæreUtfylt.left()
        }

        return meldekortBehandling.underkjenn(begrunnelse, command.saksbehandler, clock)
            .onRight { meldekortBehandlingRepo.oppdater(it) }
    }
}
