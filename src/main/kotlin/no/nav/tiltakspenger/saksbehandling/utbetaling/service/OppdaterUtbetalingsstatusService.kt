package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Ment å kalles fra en jobb.
 * Oppdaterer status på utbetalinger.
 */
class OppdaterUtbetalingsstatusService(
    private val utbetalingRepo: UtbetalingRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun oppdaterUtbetalingsstatus() {
        Either.catch {
            utbetalingRepo.hentDeSomSkalHentesUtbetalingsstatusFor().forEach {
                it.forsøkshistorikk.let { forsøkshistorikk ->
                    val (forrigeForsøk, _, antallForsøk) = forsøkshistorikk
                    forrigeForsøk?.let { forrigeForsøk ->
                        val kanPrøvePåNyttNå = forrigeForsøk.shouldRetry(antallForsøk, clock).first
                        if (!kanPrøvePåNyttNå) {
                            return@forEach
                        }
                    }
                }

                oppdaterEnkel(it)
            }
        }.onLeft {
            with("Uventet feil ved oppdatering av utbetalingsstatus.") {
                logger.error(it) { this }
            }
        }
    }

    private suspend fun oppdaterEnkel(utbetaling: UtbetalingDetSkalHentesStatusFor) {
        Either.catch {
            utbetalingsklient.hentUtbetalingsstatus(utbetaling).onRight {
                val forsøkshistorikk = if (it.erOK()) {
                    utbetaling.forsøkshistorikk
                } else {
                    utbetaling.forsøkshistorikk.inkrementer(clock)
                }

                utbetalingRepo.oppdaterUtbetalingsstatus(utbetaling.utbetalingId, it, forsøkshistorikk)
                logger.info { "Oppdatert utbetalingsstatus til $it. Kontekst: $utbetaling" }

                if (it == Utbetalingsstatus.FeiletMotOppdrag) {
                    logger.error { "Utbetaling $utbetaling feilet mot oppdrag med status $it. Dette må følges opp manuelt. Denne blir ikke prøvd på nytt." }
                    MetricRegister.UTBETALING_FEILET.inc()
                } else if (!it.erOK() &&
                    ChronoUnit.DAYS.between(
                        utbetaling.sendtTilUtbetalingstidspunkt,
                        LocalDateTime.now(clock),
                    ) >= 3
                ) {
                    // Vi gir en varsling til utviklerne hvis vi ikke har fått OK ila. 3 dager.
                    logger.error { "Utbetaling $utbetaling har ikke fått OK-status ila. 3 dager. Dette burde følges opp manuelt. Denne blir prøvd på nytt." }
                    MetricRegister.UTBETALING_IKKE_OK.inc()
                }
            }.onLeft {
                // Dette logges fra klienten.
            }
        }.onLeft {
            with("Uventet feil ved oppdatering av utbetalingsstatus for $utbetaling.") {
                logger.error(it) { this }
            }
        }
    }
}
