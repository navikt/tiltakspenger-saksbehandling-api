package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
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
                    val forrigeForsøk = utbetalingsvedtak.statusMetadata.forrigeForsøk
                    val antallForsøk = utbetalingsvedtak.statusMetadata.antallForsøk
                    val kanPrøvePåNyttNå = forrigeForsøk.shouldRetry(antallForsøk, clock).first

                    if (!kanPrøvePåNyttNå) {
                        return@forEach
                    }

                    val forrigeUtbetalingJson =
                        utbetalingsvedtak.forrigeUtbetalingsvedtakId?.let { forrigeUtbetalingsvedtakId ->
                            utbetalingsvedtakRepo.hentUtbetalingJsonForVedtakId(forrigeUtbetalingsvedtakId)
                        }

                    utbetalingsklient.iverksett(utbetalingsvedtak, forrigeUtbetalingJson, correlationId)
                        .onRight {
                            logger.info { "Utbetaling iverksatt for vedtak ${utbetalingsvedtak.id}" }
                            utbetalingsvedtakRepo.markerSendtTilUtbetaling(utbetalingsvedtak.id, nå(clock), it)
                            logger.info { "Utbetaling markert som utbetalt for vedtak ${utbetalingsvedtak.id}" }
                        }
                        .onLeft {
                            logger.error { "Utbetaling kunne ikke iverksettes. Saksnummer: ${utbetalingsvedtak.saksnummer}, sakId: ${utbetalingsvedtak.sakId}, utbetalingsvedtakId: ${utbetalingsvedtak.id}" }
                            utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(
                                vedtakId = utbetalingsvedtak.id,
                                utbetalingsrespons = it,
                                forsøkshistorikk = utbetalingsvedtak.statusMetadata.inkrementer(clock),
                            )
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
