package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta.KanIkkeTaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.ta.taMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class TaMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }
    fun taMeldekortbehandling(
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

        val harOvertatt = when (oppdatert.status) {
            MeldekortbehandlingStatus.UNDER_BEHANDLING -> meldekortbehandlingRepo.taBehandlingSaksbehandler(
                oppdatert.id,
                saksbehandler,
                oppdatert.status,
                oppdatert.sistEndret,
            )

            MeldekortbehandlingStatus.UNDER_BESLUTNING -> meldekortbehandlingRepo.taBehandlingBeslutter(
                oppdatert.id,
                saksbehandler,
                oppdatert.status,
                oppdatert.sistEndret,
            )

            else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status etter ta: ${oppdatert.status}")
        }

        require(harOvertatt) {
            "Oppdatering av saksbehandler i db feilet ved ta meldekortbehandling for $meldekortId"
        }

        logger.info { "Saksbehandler/beslutter ${saksbehandler.navIdent} tok meldekortbehandling $meldekortId" }

        return (sak.oppdaterMeldekortbehandling(oppdatert) to oppdatert).right()
    }
}
