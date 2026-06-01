package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Har ansvar for å ta imot et utfylt meldekort og lagre det.
 */
class OppdaterMeldekortbehandlingService(
    private val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    private val sakService: SakService,
    private val simulerService: SimulerService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun oppdaterMeldekort(
        kommando: OppdaterMeldekortbehandlingKommando,
        clock: Clock,
    ): Either<KanIkkeOppdatereMeldekortbehandling, Pair<Sak, MeldekortUnderBehandling>> {
        val sak = sakService.hentForSakId(kommando.sakId)

        return sak.oppdaterMeldekort(
            kommando = kommando,
            simuler = { behandling ->
                simulerService.simulerMeldekort(
                    behandling = behandling,
                    forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
                    meldeperiodeKjeder = sak.meldeperiodeKjeder,
                    brukersNavkontor = { behandling.navkontor },
                    kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
                )
            },
            clock = clock,
        ).map { (sak, meldekort, simulering) ->
            meldekortbehandlingRepo.oppdater(meldekort, simulering)
            logger.info { "Meldekort under behandling med id ${meldekort.id} oppdatert. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            Pair(sak, meldekort)
        }
    }
}
