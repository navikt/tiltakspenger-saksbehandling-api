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
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.tilStatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ProdusertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingProducer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilHelvedUtbetalingJson
import java.time.Clock

/**
 * Har ansvar for å sende klare utbetalinger til helved utsjekk.
 *
 * For V2 lagres statistikken og markeringen som sendt i samme transaksjon som HTTP-kallet til Hel Ved.
 * Feiler kallet, rulles transaksjonen tilbake, og et vellykket kall som ikke ble registrert i databasen prøves igjen med Hel Veds deduplisering.
 * For V3 lagres request-strengen først i en egen fullført transaksjon, før statistikk, sendt-markering og Kafka-produksjon skjer i en ny transaksjon.
 * Hvis Kafka bekrefter meldingen, men sendetransaksjonen feiler, bruker neste forsøk samme lagrede JSON byte for byte og samme utbetalingId som Kafka-nøkkel.
 */
class SendUtbetalingerService(
    private val utbetalingRepo: UtbetalingRepo,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val utbetalingsklient: Utbetalingsklient,
    private val utbetalingProducer: UtbetalingProducer,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
    private val korrelasjon: Korrelasjon,
) {
    val logger = KotlinLogging.logger { }

    suspend fun sendUtbetalingerTilHelved() {
        Either.catch {
            utbetalingRepo.hentForUtsjekk().forEach { utbetaling ->
                korrelasjon.medNyCorrelationId { correlationId ->
                    // correlationId sendes som Nav-Call-Id til helved, så den kan brukes til å korrelere med loggene deres.
                    val kontekst = "${utbetaling.enkelLoggKontekst}, correlationId: ${correlationId.value}"
                    Either.catch {
                        sendUtbetaling(utbetaling, correlationId, kontekst)
                    }.onLeft {
                        logger.error(it) { "Ukjent feil ved iverksetting av utbetaling mot helved. $kontekst" }
                    }
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

        if (utbetalingRepo.hentOppdragsversjon(utbetaling.sakId) == Oppdragsversjon.V3) {
            produserUtbetaling(utbetaling, forrigeUtbetalingJson, statistikk, kontekst)
            return
        }

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

    /**
     * Et unntak ruller tilbake transaksjonen, og utbetalingen prøves på nytt ved neste kjøring.
     * Requesten er allerede lagret i en egen transaksjon, så et nytt forsøk bruker samme JSON byte for byte og samme [UtbetalingId] som Kafka-nøkkel.
     */
    private fun produserUtbetaling(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        statistikk: StatistikkDTO,
        kontekst: String,
    ) {
        val helvedUtbetalingJson = utbetalingRepo.hentUtbetalingJson(utbetaling.id)
            ?: utbetaling.tilHelvedUtbetalingJson(forrigeUtbetalingJson).let { kandidat ->
                sessionFactory.withTransactionContext { tx ->
                    utbetalingRepo.lagreUtbetalingJsonHvisMangler(utbetaling.id, kandidat, tx)
                } ?: error("Fant ikke utbetaling ${utbetaling.id} da V3-requesten skulle lagres")
            }

        sessionFactory.withTransactionContext { tx ->
            statistikkService.lagre(statistikk, tx)
            utbetalingRepo.markerSendtTilUtbetaling(utbetaling.id, nå(clock), tx)
            utbetalingRepo.lagreUtbetalingsrespons(utbetaling.id, ProdusertUtbetaling(helvedUtbetalingJson), tx)
            utbetalingProducer.produser(utbetaling.id, helvedUtbetalingJson)
        }
        logger.info { "Produserte utbetaling til Hel Ved på Kafka, markert som sendt til utbetaling. $kontekst. Se sikkerlogg for detaljer." }
        Sikkerlogg.info { "Produserte utbetaling til Hel Ved på Kafka. $kontekst. Request: $helvedUtbetalingJson" }
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
