package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import java.time.Clock

/**
 * Har ansvar for å sende klare utbetalingsvedtak til helved utsjekk.
 */
class SendUtbetalingerService(
    private val utbetalingRepo: UtbetalingRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    suspend fun send() {
        Either.catch {
            utbetalingRepo.hentForUtsjekk().forEach { utbetaling ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    val forrigeUtbetalingJson =
                        utbetaling.forrigeUtbetalingId?.let { utbetalingRepo.hentUtbetalingJson(it) }

                    utbetalingsklient.iverksett(utbetaling, forrigeUtbetalingJson, correlationId).onRight {
                        logger.info { "Utbetaling iverksatt for ${utbetaling.id} / vedtak ${utbetaling.vedtakId}" }
                        utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, nå(clock), it)
                        logger.info { "Utbetaling markert som utbetalt for utbetaling ${utbetaling.id}" }
                    }.onLeft {
                        logger.error { "Utbetaling kunne ikke iverksettes. Saksnummer: ${utbetaling.saksnummer}, sakId: ${utbetaling.sakId}, utbetalingsvedtakId: ${utbetaling.id}" }
                        utbetalingRepo.lagreFeilResponsFraUtbetaling(utbetaling.id, it)
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under iverksetting av utbetaling. Saksnummer: ${utbetaling.saksnummer}, sakId: ${utbetaling.sakId}, utbetalingsvedtakId: ${utbetaling.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under utbetaling mot helved" }
        }
    }
}
