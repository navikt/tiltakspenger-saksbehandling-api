package no.nav.tiltakspenger.saksbehandling.datadeling

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.sammenlign
import no.nav.tiltakspenger.saksbehandling.dokument.infra.toBeregningSammenligningDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import java.time.Clock

class SendTilDatadelingService(
    private val rammevedtakRepo: RammevedtakRepo,
    private val behandlingRepo: BehandlingRepo,
    private val sakRepo: SakRepo,
    private val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    private val meldekortvedtakRepo: MeldekortvedtakRepo,
    private val datadelingClient: DatadelingClient,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    // vi venter med å dele godkjente meldekort til formatet er oppdatert i tiltakspenger-datadeling
    suspend fun send() {
        sendSak()
        sendBehandlinger()
        sendMeldekortbehandlinger()
        sendVedtak()
        sendMeldeperioder()
        // sendGodkjenteMeldekort()
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
                        datadelingClient.send(meldeperioder, correlationId).onRight {
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
            meldekortvedtakRepo.hentMeldekortvedtakTilDatadeling().forEach { meldekortvedtak ->
                val correlationId = CorrelationId.generate()
                val totalDifferanse = if (meldekortvedtak.erKorrigering) {
                    getTotalDifferanseForKorrigering(meldekortvedtak)
                } else {
                    null
                }
                Either.catch {
                    datadelingClient.send(meldekortvedtak, totalDifferanse, correlationId).onRight {
                        logger.info { "Meldekort sendt til datadeling. MeldekortvedtakId: ${meldekortvedtak.id}, sakId: ${meldekortvedtak.sakId}" }
                        meldekortvedtakRepo.markerSendtTilDatadeling(meldekortvedtak.id, nå(clock))
                        logger.info { "Meldekort med vedtakid ${meldekortvedtak.id} markert som sendt til datadeling. SakId: ${meldekortvedtak.sakId}" }
                    }.onLeft {
                        // Disse logges av klienten, trenger ikke duplikat logglinje.
                    }
                }.onLeft {
                    logger.error(it) { "Ukjent feil skjedde under sending av meldekort med meldekortvedtakId ${meldekortvedtak.id} til datadeling. Saksnummer: ${meldekortvedtak.saksnummer}, sakId: ${meldekortvedtak.sakId}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under henting av meldekort som skal sendes til datadeling." }
        }
    }

    private fun getTotalDifferanseForKorrigering(meldekortvedtak: Meldekortvedtak): Int {
        val sak = sakRepo.hentForSakId(meldekortvedtak.sakId)
            ?: throw IllegalStateException("Fant ikke sak med id ${meldekortvedtak.sakId} ved beregning av differanse")
        val sammenligning = { beregningEtter: MeldeperiodeBeregning ->
            val beregningFør = sak.meldeperiodeBeregninger.hentForrigeBeregning(
                beregningEtter.id,
                beregningEtter.kjedeId,
            ).getOrElse {
                when (it) {
                    MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.IngenTidligereBeregninger -> null
                    MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.IngenBeregningerForKjede,
                    MeldeperiodeBeregningerVedtatt.ForrigeBeregningFinnesIkke.BeregningFinnesIkke,
                    -> {
                        logger.error { "Fant ikke beregningen ${beregningEtter.id} på kjede ${beregningEtter.kjedeId} - Dette er sannsynligvis en feil!" }
                        null
                    }
                }
            }
            sammenlign(beregningFør, beregningEtter)
        }
        return meldekortvedtak.toBeregningSammenligningDTO(sammenligning).totalDifferanse
    }
}
