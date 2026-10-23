package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta.KanIkkeTaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta.taMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.meldekortbehandling.genererSaksstatistikk
import java.time.Clock

class TaMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun taMeldekortbehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Either<KanIkkeTaMeldekortbehandling, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val meldekortbehandling: Meldekortbehandling =
            sak.hentMeldekortbehandling(meldekortId)
                ?: return KanIkkeTaMeldekortbehandling.MeldekortbehandlingFinnesIkke.left()

        val oppdatert = meldekortbehandling.taMeldekortbehandling(saksbehandler, clock)
            .getOrElse { return it.left() }

        val statistikk = statistikkService.generer(
            sak.genererSaksstatistikk(oppdatert, StatistikkhendelseType.OPPDATERT_SAKSBEHANDLER_BESLUTTER),
        )

        sessionFactory.withTransactionContext { tx ->
            val harOvertatt = when (oppdatert.status) {
                MeldekortbehandlingStatus.UNDER_BEHANDLING -> meldekortbehandlingRepo.taBehandlingSaksbehandler(
                    oppdatert,
                    tx,
                )

                MeldekortbehandlingStatus.UNDER_BESLUTNING -> meldekortbehandlingRepo.taBehandlingBeslutter(
                    oppdatert.id,
                    saksbehandler,
                    oppdatert.status,
                    oppdatert.sistEndret,
                    tx,
                )

                else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status etter ta: ${oppdatert.status}")
            }

            require(harOvertatt) {
                "Oppdatering av saksbehandler i db feilet ved ta meldekortbehandling for $meldekortId"
            }
            statistikkService.lagre(statistikk, tx)
        }

        logger.info { "Saksbehandler/beslutter ${saksbehandler.navIdent} tok meldekortbehandling $meldekortId" }

        return (sak.oppdaterMeldekortbehandling(oppdatert) to oppdatert).right()
    }
}
