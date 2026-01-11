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
import java.time.Clock

class LeggTilbakeMeldekortBehandlingService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    fun leggTilbakeMeldekortBehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, MeldekortBehandling> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val meldekortBehandling: MeldekortBehandling = sak.hentMeldekortBehandling(meldekortId)!!
        return meldekortBehandling.leggTilbakeMeldekortBehandling(saksbehandler, clock).let {
            when (it.status) {
                MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING -> meldekortBehandlingRepo.leggTilbakeBehandlingSaksbehandler(
                    it.id,
                    saksbehandler,
                    it.status,
                    it.sistEndret,
                )

                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> meldekortBehandlingRepo.leggTilbakeBehandlingBeslutter(
                    it.id,
                    saksbehandler,
                    it.status,
                    it.sistEndret,
                )

                else -> throw IllegalStateException("Meldekortbehandlingen er i en ugyldig status for å kunne legge tilbake")
            }
            sak.oppdaterMeldekortbehandling(it) to it
        }
    }
}
