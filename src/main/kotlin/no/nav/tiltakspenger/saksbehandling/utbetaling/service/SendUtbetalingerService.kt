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
            utbetalingsvedtakRepo.hentUtbetalingsvedtakForUtsjekk().forEach { meldekortVedtak ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    val forrigeUtbetalingJson =
                        meldekortVedtak.utbetaling.forrigeUtbetalingVedtakId?.let { forrigeUtbetalingVedtakId ->
                            utbetalingsvedtakRepo.hentUtbetalingJsonForVedtakId(forrigeUtbetalingVedtakId)
                        }
                    utbetalingsklient.iverksett(meldekortVedtak, forrigeUtbetalingJson, correlationId).onRight {
                        logger.info { "Utbetaling iverksatt for vedtak ${meldekortVedtak.id}" }
                        utbetalingsvedtakRepo.markerSendtTilUtbetaling(meldekortVedtak.id, nå(clock), it)
                        logger.info { "Utbetaling markert som utbetalt for vedtak ${meldekortVedtak.id}" }
                    }.onLeft {
                        logger.error { "Utbetaling kunne ikke iverksettes. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, utbetalingsvedtakId: ${meldekortVedtak.id}" }
                        utbetalingsvedtakRepo.lagreFeilResponsFraUtbetaling(meldekortVedtak.id, it)
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under iverksetting av utbetaling. Saksnummer: ${meldekortVedtak.saksnummer}, sakId: ${meldekortVedtak.sakId}, utbetalingsvedtakId: ${meldekortVedtak.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under utbetaling mot helved" }
        }
    }
}
