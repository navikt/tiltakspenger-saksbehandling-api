package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.OvertaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OvertaMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
) {
    fun overta(
        command: OvertaMeldekortbehandlingKommando,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(command.meldekortId)!!

        return meldekortbehandling.overta(command.saksbehandler, clock).map {
            when (it.status) {
                MeldekortbehandlingStatus.UNDER_BEHANDLING -> meldekortbehandlingRepo.overtaSaksbehandler(
                    meldekortId = it.id,
                    nySaksbehandler = command.saksbehandler,
                    nåværendeSaksbehandler = command.overtarFra,
                    it.sistEndret,
                )

                MeldekortbehandlingStatus.UNDER_BESLUTNING -> meldekortbehandlingRepo.overtaBeslutter(
                    meldekortId = it.id,
                    nyBeslutter = command.saksbehandler,
                    nåværendeBeslutter = command.overtarFra,
                    it.sistEndret,
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
