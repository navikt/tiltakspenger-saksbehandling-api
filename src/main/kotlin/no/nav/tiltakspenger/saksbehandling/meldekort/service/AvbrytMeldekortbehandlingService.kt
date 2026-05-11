package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.AvbrytMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.KanIkkeAvbryteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.avbryt
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class AvbrytMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
) {
    fun avbryt(command: AvbrytMeldekortbehandlingKommando): Either<KanIkkeAvbryteMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortbehandling = sak.hentMeldekortbehandling(command.meldekortId)

        if (meldekortbehandling !is MeldekortUnderBehandling) {
            return KanIkkeAvbryteMeldekortbehandling.MåVæreUnderBehandling.left()
        }

        return meldekortbehandling.avbryt(command.saksbehandler, command.begrunnelse, nå(clock)).map {
            meldekortbehandlingRepo.oppdater(it)
            Pair(sak.oppdaterMeldekortbehandling(it), it)
        }
    }
}
