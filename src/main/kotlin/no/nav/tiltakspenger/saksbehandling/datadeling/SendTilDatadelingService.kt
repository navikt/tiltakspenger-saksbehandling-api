package no.nav.tiltakspenger.saksbehandling.datadeling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import java.time.Clock

class SendTilDatadelingService(
    private val rammevedtakRepo: RammevedtakRepo,
    private val behandlingRepo: BehandlingRepo,
    private val sakRepo: SakRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val datadelingClient: DatadelingClient,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun send() {
        sendSak()
        sendBehandlinger()
        sendMeldekortbehandlinger()
        sendVedtak()
        sendMeldeperioder()
        sendGodkjenteMeldekort()
    }

    private suspend fun sendSak() {
        Either.catch {
            sakRepo.hentSakerTilDatadeling().forEach { sakDb ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    datadelingClient.send(sakDb, correlationId).onRight {
                        logger.info { "Sak sendt til datadeling. SakId: ${sakDb.id}" }
                        sakRepo.markerSendtTilDatadeling(sakDb.id, nå(clock))
                        logger.info { "Sak med id ${sakDb.id} markert som sendt til datadeling." }
                    }.onLeft {
                        // Disse logges av klienten, trenger ikke duplikat logglinje.
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av sak med id ${sakDb.id} til datadeling." }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av saker som skal sendes til datadeling." }
        }
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
            behandlingRepo.hentBehandlingerTilDatadeling().forEach { behandling ->
                val correlationId = CorrelationId.generate()

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

    private suspend fun sendMeldekortbehandlinger() {
        Either.catch {
            meldekortBehandlingRepo.hentBehandlingerTilDatadeling().forEach { meldekortbehandling ->
                val correlationId = CorrelationId.generate()

                Either.catch {
                    datadelingClient.send(meldekortbehandling, correlationId).onRight {
                        logger.info { "Meldekortbehandling sendt til datadeling. Saksnummer: ${meldekortbehandling.saksnummer}, sakId: ${meldekortbehandling.sakId}, meldekortbehandlingId: ${meldekortbehandling.id}" }
                        meldekortBehandlingRepo.markerBehandlingSendtTilDatadeling(meldekortbehandling.id, nå(clock))
                        logger.info { "Meldekortbehandling markert som sendt til datadeling. Saksnummer: ${meldekortbehandling.saksnummer}, sakId: ${meldekortbehandling.sakId}, meldekortbehandlingId: ${meldekortbehandling.id}" }
                    }.onLeft {
                        logger.error { "Meldekortbehandling kunne ikke sendes til datadeling. Saksnummer: ${meldekortbehandling.saksnummer}, sakId: ${meldekortbehandling.sakId}, meldekortbehandlingId: ${meldekortbehandling.id}" }
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av meldekortbehandling til datadeling. Saksnummer: ${meldekortbehandling.saksnummer}, sakId: ${meldekortbehandling.sakId}, meldekortbehandlingId: ${meldekortbehandling.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av meldekortbehandling som skal sendes til datadeling." }
        }
    }

    private suspend fun sendMeldeperioder() {
        Either.catch {
            sakRepo.hentForSendingAvMeldeperioderTilDatadeling().forEach { sak ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    val meldeperioder = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede
                    if (meldeperioder.isNotEmpty()) {
                        datadelingClient.send(sak, meldeperioder, correlationId).onRight {
                            logger.info { "Meldeperioder sendt til datadeling. SakId: ${sak.id}" }
                            sakRepo.oppdaterSkalSendeMeldeperioderTilDatadeling(
                                sak.id,
                                skalSendeMeldeperioderTilDatadeling = false,
                            )
                            logger.info { "Meldeperioder markert som sendt til datadeling. SakId: ${sak.id}" }
                        }.onLeft {
                            // Disse logges av klienten, trenger ikke duplikat logglinje.
                        }
                    } else {
                        logger.warn { "Sak med id ${sak.id} har ingen meldeperioder som kan deles" }
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av meldeperioder til datadeling. Saksnummer: ${sak.saksnummer}, sakId: ${sak.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av saker med meldeperioder som skal sendes til datadeling." }
        }
    }

    private suspend fun sendGodkjenteMeldekort() {
        Either.catch {
            meldekortBehandlingRepo.hentGodkjenteMeldekortTilDatadeling().forEach { godkjentMeldekort ->
                val correlationId = CorrelationId.generate()
                Either.catch {
                    datadelingClient.send(godkjentMeldekort, clock, correlationId).onRight {
                        logger.info { "Meldekort sendt til datadeling. MeldekortId: ${godkjentMeldekort.id}, sakId: ${godkjentMeldekort.sakId}" }
                        meldekortBehandlingRepo.markerSendtTilDatadeling(godkjentMeldekort.id, nå(clock))
                        logger.info { "Meldekort med id ${godkjentMeldekort.id} markert som sendt til datadeling. SakId: ${godkjentMeldekort.sakId}" }
                    }.onLeft {
                        // Disse logges av klienten, trenger ikke duplikat logglinje.
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av meldekort med id ${godkjentMeldekort.id} til datadeling. Saksnummer: ${godkjentMeldekort.saksnummer}, sakId: ${godkjentMeldekort.sakId}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av meldekort som skal sendes til datadeling." }
        }
    }
}
