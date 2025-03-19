package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.UtbetalingGateway
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ment å kalles fra en jobb.
 * Oppdaterer status på utbetalinger.
 */
class OppdaterUtbetalingsstatusService(
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val utbetalingGateway: UtbetalingGateway,
) {
    private val logger = KotlinLogging.logger { }

    // TODO jah: Sletter når vi har sjekket at klienten fungerer.
    private val harFeilet = AtomicBoolean(false)

    suspend fun oppdaterUtbetalingsstatus() {
        Either.catch {
            if (harFeilet.get()) return
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor().forEach {
                if (harFeilet.get()) return
                oppdaterEnkel(it)
            }
        }.onLeft {
            harFeilet.set(true)
            with("Uventet feil ved oppdatering av utbetalingsstatus.") {
                logger.error(RuntimeException("Trigger stacktrace for enklere debug")) { "$this Se sikkerlogg for mer kontekst." }
                sikkerlogg.error(it) { this }
            }
        }
    }

    private suspend fun oppdaterEnkel(utbetaling: UtbetalingDetSkalHentesStatusFor) {
        Either.catch {
            utbetalingGateway.hentUtbetalingsstatus(utbetaling).onRight {
                utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(utbetaling.vedtakId, it)
                logger.info { "Oppdatert utbetalingsstatus til $it. Kontekst: $utbetaling" }
            }.onLeft {
                harFeilet.set(true)
                // Dette logges fra klienten.
            }
        }.onLeft {
            harFeilet.set(true)
            with("Uventet feil ved oppdatering av utbetalingsstatus for $utbetaling.") {
                logger.error(RuntimeException("Trigger stacktrace for enklere debug")) { "$this Se sikkerlogg for mer kontekst." }
                sikkerlogg.error(it) { this }
            }
        }
    }
}
