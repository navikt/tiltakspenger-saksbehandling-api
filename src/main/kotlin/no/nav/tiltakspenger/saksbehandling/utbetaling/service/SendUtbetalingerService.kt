package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.tilStatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import java.time.Clock

/**
 * Har ansvar for å sende klare utbetalinger til helved utsjekk.
 */
class SendUtbetalingerService(
    private val utbetalingRepo: UtbetalingRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val statistikkService: StatistikkService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun sendUtbetalingerTilHelved() {
        Either.catch {
            utbetalingRepo.hentForUtsjekk().forEach { utbetaling ->
                val correlationId = CorrelationId.generate()
                // correlationId sendes som Nav-Call-Id til helved, så den kan brukes til å korrelere med loggene deres.
                val kontekst = "${utbetaling.enkelLoggKontekst}, correlationId: ${correlationId.value}"
                Either.catch {
                    val forrigeUtbetalingJson =
                        utbetaling.forrigeUtbetalingId?.let { utbetalingRepo.hentUtbetalingJson(it) }

                    utbetalingsklient.iverksett(utbetaling, forrigeUtbetalingJson, correlationId).onRight {
                        if (it.alleredeMottattTidligere) {
                            logger.info { "Iverksetting av utbetaling mot helved: allerede mottatt fra et tidligere forsøk, behandles som vanlig suksess. $kontekst. Se sikkerlogg for detaljer." }
                        }
                        logger.info { "Iverksetting av utbetaling mot helved OK. $kontekst" }
                        Sikkerlogg.info { "Iverksetting av utbetaling mot helved OK. $kontekst. Response: ${it.response}. Request: ${it.request}" }

                        utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, nå(clock), it)
                        logger.info { "Iverksetting av utbetaling mot helved: markert som sendt til utbetaling. $kontekst" }

                        statistikkService.lagre(utbetaling.tilStatistikk(clock), null)
                    }.onLeft { kunneIkkeUtbetale ->
                        kunneIkkeUtbetale.feil.loggFeil(logger, "iverksetting av utbetaling mot helved", "$kontekst. Denne vil bli prøvd på nytt.")
                        utbetalingRepo.lagreFeilResponsFraUtbetaling(utbetaling.id, kunneIkkeUtbetale)
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil ved iverksetting av utbetaling mot helved. $kontekst" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil ved iverksetting av utbetaling mot helved" }
        }
    }
}
