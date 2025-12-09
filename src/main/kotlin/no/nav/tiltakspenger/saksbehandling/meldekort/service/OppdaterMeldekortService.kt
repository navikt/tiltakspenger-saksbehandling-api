package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.oppdaterMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Har ansvar for å ta imot et utfylt meldekort og lagre det.
 */
class OppdaterMeldekortService(
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val sakService: SakService,
    private val simulerService: SimulerService,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun oppdaterMeldekort(
        kommando: OppdaterMeldekortKommando,
    ): Either<KanIkkeOppdatereMeldekort, Pair<Sak, MeldekortUnderBehandling>> {
        val sak = hentSak(kommando)
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
            meldekortBehandlingRepo.oppdater(meldekort, simulering)
            logger.info { "Meldekort under behandling med id ${meldekort.id} oppdatert. Saksbehandler: ${kommando.saksbehandler.navIdent}" }
            Pair(sak, meldekort)
        }
    }

    // TODO jah: Kopiert til [SendMeldekortTilBeslutterService] - lage noe felles?
    private fun hentSak(
        kommando: OppdaterMeldekortKommando,
    ): Sak {
        val sak = sakService.hentForSakId(kommando.sakId)

        val meldekortbehandling = sak.hentMeldekortBehandling(kommando.meldekortId)!!
        val meldeperiode = meldekortbehandling.meldeperiode
        if (!sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            throw IllegalStateException("Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: ${sak.id}, meldekortId: ${meldekortbehandling.id}")
        }
        return sak
    }
}
