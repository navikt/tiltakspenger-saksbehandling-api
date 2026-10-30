package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.tilStatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.Clock

/**
 * Har ansvar for å sende klare utbetalinger til helved utsjekk.
 *
 * Statistikken og markeringen som sendt skrives i samme transaksjon som kallet til helved, og transaksjonen rulles tilbake dersom kallet feiler.
 * Dermed kan vi ikke ende opp med en utbetaling som er markert som sendt uten statistikk, eller med statistikk for en utbetaling som ikke er sendt.
 * Lykkes kallet, men committen feiler, prøves utbetalingen på nytt, og helved svarer da at den er mottatt fra før ([no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SendtUtbetaling.alleredeMottattTidligere]).
 */
class SendUtbetalingerService(
    private val utbetalingRepo: UtbetalingRepo,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
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
                    sendUtbetaling(utbetaling, correlationId, kontekst)
                }.onLeft {
                    logger.error(it) { "Ukjent feil ved iverksetting av utbetaling mot helved. $kontekst" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil ved iverksetting av utbetaling mot helved" }
        }
    }

    private suspend fun sendUtbetaling(
        utbetaling: VedtattUtbetaling,
        correlationId: CorrelationId,
        kontekst: String,
    ) {
        val forrigeUtbetalingJson = utbetaling.forrigeUtbetalingId?.let { utbetalingRepo.hentUtbetalingJson(it) }
        val statistikk = statistikkService.generer(utbetaling.tilStatistikk(hentRammevedtakIder(utbetaling), clock))

        val sendtUtbetaling = try {
            sessionFactory.withTransactionContext { tx ->
                statistikkService.lagre(statistikk, tx)
                utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, nå(clock), tx)

                // withTransactionContext tar ikke en suspend-lambda; samme mønster som ved iverksetting av rammebehandling.
                val sendt = runBlocking {
                    utbetalingsklient.iverksett(utbetaling, forrigeUtbetalingJson, correlationId)
                }.getOrElse { throw IverksettingFeilet(it) }

                utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, sendt, tx)
                sendt
            }
        } catch (e: IverksettingFeilet) {
            e.kunneIkkeUtbetale.feil.loggFeil(
                logger,
                "iverksetting av utbetaling mot helved",
                "$kontekst. Denne vil bli prøvd på nytt.",
            )
            utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, e.kunneIkkeUtbetale)
            return
        }

        if (sendtUtbetaling.alleredeMottattTidligere) {
            logger.info { "Iverksetting av utbetaling mot helved: allerede mottatt fra et tidligere forsøk, behandles som vanlig suksess. $kontekst. Se sikkerlogg for detaljer." }
        }
        logger.info { "Iverksetting av utbetaling mot helved OK, markert som sendt til utbetaling. $kontekst" }
        Sikkerlogg.info { "Iverksetting av utbetaling mot helved OK. $kontekst. Response: ${sendtUtbetaling.response}. Request: ${sendtUtbetaling.request}" }
    }

    // Foreign key-en utbetaling_meldekortvedtak_id_fkey garanterer at meldekortvedtaket finnes.
    private fun hentRammevedtakIder(utbetaling: VedtattUtbetaling): List<VedtakId> =
        when (utbetaling.beregningKilde) {
            is BeregningKilde.BeregningKildeRammebehandling -> listOf(utbetaling.vedtakId)
            is BeregningKilde.BeregningKildeMeldekort -> meldekortvedtakRepo.hentForVedtakId(utbetaling.vedtakId)!!.rammevedtak
        }

    /**
     * Kastes inne i transaksjonen for å rulle den tilbake når helved svarer med feil.
     */
    private class IverksettingFeilet(val kunneIkkeUtbetale: KunneIkkeUtbetale) : RuntimeException()
}
