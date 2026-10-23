package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre.KanIkkeAngreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.angre.angreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class AngreMeldekortbehandlingService(
    private val sakService: SakService,
    private val clock: Clock,
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {

    val logger = KotlinLogging.logger { }

    suspend fun angreMeldekortbehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Either<KanIkkeAngreMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)

        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(meldekortId)
            ?: return KanIkkeAngreMeldekortbehandling.MeldekortbehandlingFinnesIkke.left()

        return meldekortbehandling.angreMeldekortbehandling(
            saksbehandler,
            clock,
        ).map { angreMeldekortbehandling ->
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(angreMeldekortbehandling, StatistikkhendelseType.SAKSBEHANDLER_ANGRER),
            )
            val angret = sessionFactory.withTransactionContext { tx ->
                meldekortbehandlingRepo.angreBehandling(
                    meldekortbehandling = angreMeldekortbehandling,
                    forventetSendtTilBeslutning = meldekortbehandling.sendtTilBeslutning,
                    transactionContext = tx,
                ).also { if (it) statistikkService.lagre(statistikk, tx) }
            }
            if (!angret) {
                return KanIkkeAngreMeldekortbehandling.MeldekortbehandlingErIkkeLengerSendtTilBeslutning.left()
            }
            sak.oppdaterMeldekortbehandling(angreMeldekortbehandling) to angreMeldekortbehandling
        }
    }
}
