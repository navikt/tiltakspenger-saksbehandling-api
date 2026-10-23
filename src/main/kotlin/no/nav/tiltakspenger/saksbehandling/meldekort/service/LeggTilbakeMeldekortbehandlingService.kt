package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake.KanIkkeLeggeTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake.leggTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class LeggTilbakeMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    val logger = KotlinLogging.logger { }

    suspend fun leggTilbakeMeldekortbehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Either<KanIkkeLeggeTilbakeMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(meldekortId)!!
        return meldekortbehandling.leggTilbakeMeldekortbehandling(saksbehandler, clock).map {
            val statistikk = statistikkService.generer(
                sak.genererSaksstatistikk(it, StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
            )
            sessionFactory.withTransactionContext { tx ->
                when (it.status) {
                    MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> meldekortbehandlingRepo.leggTilbakeBehandlingSaksbehandler(
                        it,
                        saksbehandler,
                        tx,
                    )

                    MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> meldekortbehandlingRepo.leggTilbakeBehandlingBeslutter(
                        it.id,
                        saksbehandler,
                        it.status,
                        it.sistEndret,
                        tx,
                    )

                    else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne legge tilbake")
                }
                statistikkService.lagre(statistikk, tx)
            }
            sak.oppdaterMeldekortbehandling(it) to it
        }
    }
}
