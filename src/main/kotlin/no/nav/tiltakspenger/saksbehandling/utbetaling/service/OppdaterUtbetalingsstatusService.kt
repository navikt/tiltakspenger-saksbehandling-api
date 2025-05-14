package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingGateway
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Ment å kalles fra en jobb.
 * Oppdaterer status på utbetalinger.
 */
class OppdaterUtbetalingsstatusService(
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val utbetalingGateway: UtbetalingGateway,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun oppdaterUtbetalingsstatus() {
        Either.catch {
            utbetalingsvedtakRepo.hentDeSomSkalHentesUtbetalingsstatusFor().forEach {
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
            utbetalingGateway.hentUtbetalingsstatus(utbetaling).onRight {
                utbetalingsvedtakRepo.oppdaterUtbetalingsstatus(
                    utbetaling.vedtakId,
                    it,
                    utbetaling.forsøkshistorikk?.inkrementer(clock) ?: Forsøkshistorikk.førsteForsøk(clock),
                )
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
