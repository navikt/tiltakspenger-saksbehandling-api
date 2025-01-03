package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.datadeling.ports.DatadelingGateway
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo

class SendTilDatadelingService(
    private val rammevedtakRepo: RammevedtakRepo,
    private val behandlingRepo: BehandlingRepo,
    private val datadelingGateway: DatadelingGateway,
) {
    val logger = KotlinLogging.logger { }

    suspend fun send(
        erNais: Boolean,
    ) {
        if (erNais) {
            sendBehandlinger()
            sendVedtak()
        }
    }

    private suspend fun sendVedtak() {
        Either.catch {
            rammevedtakRepo.hentRammevedtakTilDatadeling().forEach { rammevedtak ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    datadelingGateway.send(rammevedtak, correlationId).onRight {
                        logger.info { "Vedtak sendt til datadeling. VedtakId: ${rammevedtak.id}" }
                        rammevedtakRepo.markerSendtTilDatadeling(rammevedtak.id, nå())
                        logger.info { "Vedtak markert som sendt til datadeling. VedtakId: ${rammevedtak.id}" }
                    }.onLeft {
                        logger.error { "Vedtak kunne ikke sendes til datadeling. Saksnummer: ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}, vedtakId: ${rammevedtak.id}" }
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av vedtak til datadeling. Saksnummer: ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}, vedtakId: ${rammevedtak.id}" }
                }
            }
        }.onLeft {
            logger.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Ukjent feil skjedde under henting av vedtak som skal sendes til datadeling." }
            sikkerlogg.error(it) { "Ukjent feil skjedde under henting av vedtak som skal sendes til datadeling." }
        }
    }

    private suspend fun sendBehandlinger() {
        Either.catch {
            // Kommentar jah: Vi avventer sending av revurderingsbehandlinger til datadeling.
            behandlingRepo.hentFørstegangsbehandlingerTilDatadeling().forEach { behandling ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    datadelingGateway.send(behandling, correlationId).onRight {
                        logger.info { "Behandling sendt til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                        behandlingRepo.markerSendtTilDatadeling(behandling.id, nå())
                        logger.info { "Behandling markert som sendt til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                    }.onLeft {
                        logger.error { "Behandling kunne ikke sendes til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av behandling til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                }
            }
        }.onLeft {
            logger.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Ukjent feil skjedde under henting av behandling som skal sendes til datadeling." }
            sikkerlogg.error(it) { "Ukjent feil skjedde under henting av behandling som skal sendes til datadeling." }
        }
    }
}
