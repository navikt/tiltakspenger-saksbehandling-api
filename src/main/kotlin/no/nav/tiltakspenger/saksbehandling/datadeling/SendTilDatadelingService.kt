package no.nav.tiltakspenger.saksbehandling.datadeling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import java.time.Clock

class SendTilDatadelingService(
    private val rammevedtakRepo: RammevedtakRepo,
    private val behandlingRepo: BehandlingRepo,
    private val datadelingClient: DatadelingClient,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun send() {
        sendBehandlinger()
        sendVedtak()
    }

    private suspend fun sendVedtak() {
        Either.catch {
            rammevedtakRepo.hentRammevedtakTilDatadeling().forEach { rammevedtak ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    datadelingClient.send(rammevedtak, correlationId).onRight {
                        logger.info { "Vedtak sendt til datadeling. VedtakId: ${rammevedtak.id}" }
                        rammevedtakRepo.markerSendtTilDatadeling(rammevedtak.id, nå(clock))
                        logger.info { "Vedtak markert som sendt til datadeling. VedtakId: ${rammevedtak.id}" }
                    }.onLeft {
                        // Disse logges av klienten, trenger ikke duplikat logglinje.
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av vedtak til datadeling. Saksnummer: ${rammevedtak.saksnummer}, sakId: ${rammevedtak.sakId}, vedtakId: ${rammevedtak.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av vedtak som skal sendes til datadeling." }
        }
    }

    private suspend fun sendBehandlinger() {
        Either.catch {
            // Kommentar jah: Vi avventer sending av revurderingsbehandlinger til datadeling.
            behandlingRepo.hentSøknadsbehandlingerTilDatadeling().forEach { behandling ->
                val correlationId = CorrelationId.generate()
                if (behandling !is Søknadsbehandling) {
                    logger.error { "Kan kun sende søknadsbehandlinger til datadeling - ${behandling.id} er ${behandling.behandlingstype}" }
                    return
                }

                Either.catch {
                    datadelingClient.send(behandling, correlationId).onRight {
                        logger.info { "Behandling sendt til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                        behandlingRepo.markerSendtTilDatadeling(behandling.id, nå(clock))
                        logger.info { "Behandling markert som sendt til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                    }.onLeft {
                        logger.error { "Behandling kunne ikke sendes til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av behandling til datadeling. Saksnummer: ${behandling.saksnummer}, sakId: ${behandling.sakId}, behandlingId: ${behandling.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av behandling som skal sendes til datadeling." }
        }
    }
}
