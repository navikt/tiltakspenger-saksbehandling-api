package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.GjenopptaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.KanIkkeGjenopptaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.gjenoppta
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class GjenopptaMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun gjenoppta(
        kommando: GjenopptaMeldekortbehandlingKommando,
    ): Either<KanIkkeGjenopptaMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        return meldekortbehandling.gjenoppta(kommando, clock).map { oppdatertMeldekortbehandling ->
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(oppdatertMeldekortbehandling, StatistikkhendelseType.BEHANDLING_GJENOPPTATT),
            )
            sessionFactory.withTransactionContext { tx ->
                meldekortbehandlingRepo.oppdater(oppdatertMeldekortbehandling, tx)
                statistikkService.lagre(statistikk, tx)
            }
            logger.info { "Meldekortbehandling med id ${oppdatertMeldekortbehandling.id} gjenopptatt. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling) to oppdatertMeldekortbehandling
        }
    }
}
