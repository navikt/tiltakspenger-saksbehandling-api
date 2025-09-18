package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class TaMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
) {
    val logger = KotlinLogging.logger { }

    fun taMeldekortBehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, MeldekortBehandling> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val meldekortBehandling: MeldekortBehandling = sak.hentMeldekortBehandling(meldekortId)!!

        return meldekortBehandling.taMeldekortBehandling(saksbehandler).let {
            when (it.status) {
                MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> throw IllegalArgumentException("Behandlingen er ikke fått en saksbehandler for å lagre behandlingen")
                MeldekortBehandlingStatus.UNDER_BEHANDLING -> meldekortBehandlingRepo.taBehandlingSaksbehandler(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                MeldekortBehandlingStatus.UNDER_BESLUTNING -> meldekortBehandlingRepo.taBehandlingBeslutter(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne tildele seg selv")
            }.also { harOvertatt ->
                require(harOvertatt) {
                    "Oppdatering av saksbehandler i db feilet ved ta meldekortbehandling for $meldekortId"
                }
            }
            sak.oppdaterMeldekortbehandling(it) to it
        }
    }
}
