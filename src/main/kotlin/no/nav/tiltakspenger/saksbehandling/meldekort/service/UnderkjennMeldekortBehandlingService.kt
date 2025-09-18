package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KunneIkkeUnderkjenneMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UnderkjennMeldekortBehandlingCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class UnderkjennMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val clock: Clock,
    private val sakService: SakService,
) {
    fun underkjenn(
        command: UnderkjennMeldekortBehandlingCommand,
    ): Either<KunneIkkeUnderkjenneMeldekortBehandling, Pair<Sak, MeldekortBehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortBehandling: MeldekortBehandling = meldekortBehandlingRepo.hent(command.meldekortId)!!

        if (meldekortBehandling !is MeldekortBehandletManuelt) {
            return KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        val begrunnelse = Either.catch { command.begrunnelse.toNonBlankString() }.getOrElse {
            return KunneIkkeUnderkjenneMeldekortBehandling.BegrunnelseMåVæreUtfylt.left()
        }
        return meldekortBehandling.underkjenn(
            besluttersBegrunnelse = begrunnelse,
            beslutter = command.saksbehandler,
            clock = clock,
        ).map {
            meldekortBehandlingRepo.oppdater(it)
            sak.oppdaterMeldekortbehandling(it) to it
        }
    }
}
