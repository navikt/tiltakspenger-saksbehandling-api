package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo

class TaMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
) {
    val logger = KotlinLogging.logger { }

    fun taMeldekortBehandling(
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): MeldekortBehandling {
        val meldekortBehandling = meldekortBehandlingRepo.hent(meldekortId)!!

        return meldekortBehandling.taMeldekortBehandling(saksbehandler).also {
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

                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                MeldekortBehandlingStatus.GODKJENT,
                MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
                MeldekortBehandlingStatus.AVBRUTT,
                -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne tildele seg selv")
            }
        }
    }
}
