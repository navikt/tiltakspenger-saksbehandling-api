package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class LeggTilbakeMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    fun leggTilbakeMeldekortbehandling(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, Meldekortbehandling> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(meldekortId)!!
        return meldekortbehandling.leggTilbakeMeldekortbehandling(saksbehandler, clock).let {
            when (it.status) {
                MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> meldekortbehandlingRepo.leggTilbakeBehandlingSaksbehandler(
                    it.id,
                    saksbehandler,
                    it.status,
                    it.sistEndret,
                )

                MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> meldekortbehandlingRepo.leggTilbakeBehandlingBeslutter(
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
