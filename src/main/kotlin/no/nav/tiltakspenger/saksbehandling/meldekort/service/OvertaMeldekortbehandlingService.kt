package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.OvertaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.overta
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class OvertaMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    suspend fun overta(
        command: OvertaMeldekortbehandlingKommando,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(command.sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(command.meldekortId)!!

        return meldekortbehandling.overta(command, clock).map {
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(it, StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            sessionFactory.withTransactionContext { tx ->
                when (it.status) {
                    MeldekortbehandlingStatus.UNDER_BEHANDLING -> meldekortbehandlingRepo.overtaSaksbehandler(
                        meldekortbehandling = it,
                        nåværendeSaksbehandler = command.overtarFra,
                        transactionContext = tx,
                    )

                    MeldekortbehandlingStatus.UNDER_BESLUTNING -> meldekortbehandlingRepo.overtaBeslutter(
                        meldekortId = it.id,
                        nyBeslutter = command.saksbehandler,
                        nåværendeBeslutter = command.overtarFra,
                        sistEndret = it.sistEndret,
                        sessionContext = tx,
                    )

                    else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne overta")
                }.also { harOvertatt ->
                    require(harOvertatt) {
                        "Oppdatering av saksbehandler i db feilet ved overta meldekortbehandling for ${command.meldekortId}"
                    }
                }
                statistikkService.lagre(statistikk, tx)
            }
            (sak.oppdaterMeldekortbehandling(it) to it)
        }
    }
}
