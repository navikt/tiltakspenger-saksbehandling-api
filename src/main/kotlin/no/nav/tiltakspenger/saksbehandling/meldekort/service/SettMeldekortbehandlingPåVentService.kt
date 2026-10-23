package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.KanIkkeSetteMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.settPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class SettMeldekortbehandlingPåVentService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun settPåVent(
        kommando: SettMeldekortbehandlingPåVentKommando,
    ): Either<KanIkkeSetteMeldekortbehandlingPåVent, Pair<Sak, Meldekortbehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        val meldekortbehandling = sak.hentMeldekortbehandling(kommando.meldekortId)!!

        return meldekortbehandling.settPåVent(kommando, clock).map { oppdatertMeldekortbehandling ->
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(oppdatertMeldekortbehandling, StatistikkhendelseType.BEHANDLING_SATT_PA_VENT),
            )
            val oppdatert = sessionFactory.withTransactionContext { tx ->
                val oppdatert = if (meldekortbehandling.status == MeldekortbehandlingStatus.UNDER_BESLUTNING) {
                    meldekortbehandlingRepo.oppdaterHvisFortsattUnderBeslutning(
                        meldekortbehandling = oppdatertMeldekortbehandling,
                        utøvendeBeslutter = kommando.saksbehandler,
                        transactionContext = tx,
                    )
                } else {
                    meldekortbehandlingRepo.oppdater(oppdatertMeldekortbehandling, tx)
                    true
                }
                if (oppdatert) statistikkService.lagre(statistikk, tx)
                oppdatert
            }
            if (!oppdatert) {
                return KanIkkeSetteMeldekortbehandlingPåVent.BehandlingenErIkkeLengerUnderBeslutning.left()
            }
            logger.info { "Meldekortbehandling med id ${oppdatertMeldekortbehandling.id} satt på vent. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling) to oppdatertMeldekortbehandling
        }
    }
}
