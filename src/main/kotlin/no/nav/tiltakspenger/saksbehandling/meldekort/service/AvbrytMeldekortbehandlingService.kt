package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.AvbrytMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.KanIkkeAvbryteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.avbryt
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class AvbrytMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    suspend fun avbryt(kommando: AvbrytMeldekortbehandlingKommando): Either<KanIkkeAvbryteMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(kommando.sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        return meldekortbehandling.avbryt(kommando, nå(clock)).map {
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(it, StatistikkhendelseType.AVSLUTTET_BEHANDLING),
            )
            sessionFactory.withTransactionContext { tx ->
                meldekortbehandlingRepo.oppdater(it, tx)
                statistikkService.lagre(statistikk, tx)
            }
            Pair(sak.oppdaterMeldekortbehandling(it), it)
        }
    }
}
