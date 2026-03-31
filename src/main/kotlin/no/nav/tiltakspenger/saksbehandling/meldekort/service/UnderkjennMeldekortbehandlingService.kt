package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn.KanIkkeUnderkjenneMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn.UnderkjennMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class UnderkjennMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val clock: Clock,
    private val sakService: SakService,
) {
    fun underkjenn(
        command: UnderkjennMeldekortbehandlingKommando,
    ): Either<KanIkkeUnderkjenneMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortbehandling: Meldekortbehandling = meldekortbehandlingRepo.hent(command.meldekortId)!!

        if (meldekortbehandling !is MeldekortbehandlingManuell) {
            return KanIkkeUnderkjenneMeldekortbehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        val begrunnelse = Either.catch { command.begrunnelse.toNonBlankString() }.getOrElse {
            return KanIkkeUnderkjenneMeldekortbehandling.BegrunnelseMåVæreUtfylt.left()
        }
        return meldekortbehandling.underkjenn(
            besluttersBegrunnelse = begrunnelse,
            beslutter = command.saksbehandler,
            clock = clock,
        ).map {
            meldekortbehandlingRepo.oppdater(it)
            sak.oppdaterMeldekortbehandling(it) to it
        }
    }
}
