package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import java.time.Clock

/**
 * Har ansvar for å sende klare utbetalingsvedtak til helved utsjekk.
 */
class SendUtbetalingerService(
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }
    suspend fun send() {
        Either.catch {
            utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk().forEach { utbetalingsvedtak ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    val forrigeUtbetalingJson =
                        utbetalingsvedtak.forrigeUtbetalingsvedtakId?.let { forrigeUtbetalingsvedtakId ->
                            utbetalingsvedtakRepo.hentUtbetalingJsonForVedtakId(forrigeUtbetalingsvedtakId)
                        }
                    utbetalingsklient.iverksett(utbetalingsvedtak, forrigeUtbetalingJson, correlationId).onRight {
                        logger.info { "Utbetaling iverksatt for vedtak ${utbetalingsvedtak.id}" }
                        utbetalingsvedtakRepo.markerSendtTilUtbetaling(utbetalingsvedtak.id, nå(clock), it)
                        logger.info { "Utbetaling markert som utbetalt for vedtak ${utbetalingsvedtak.id}" }
                    }.onLeft {
                        logger.error { "Utbetaling kunne ikke iverksettes. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                        utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(utbetalingsvedtak.id, it)
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under iverksetting av utbetaling. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under utbetaling mot helved" }
        }
    }
}
