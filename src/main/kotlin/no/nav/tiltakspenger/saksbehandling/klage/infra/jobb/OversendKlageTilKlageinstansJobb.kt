package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentJournalpostIdForVedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandlingerSomSkalOversendesKlageinstansen
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.FeilVedOversendelseTilKabal
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansFeilet
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansenTidspunkt
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk

class OversendKlageTilKlageinstansJobb(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val sakService: SakService,
    private val kabalClient: KabalClient,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Oversender alle klagebehandlinger med status [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT] til klageinstansen.
     * Oppdaterer status og tidspunkt.
     */
    suspend fun oversendKlagerTilKlageinstans() {
        klagebehandlingRepo.hentSakerSomSkalOversendesKlageinstansen().forEach { sakId ->
            Either.runCatching {
                val sak: Sak = sakService.hentForSakId(sakId)
                sak.hentKlagebehandlingerSomSkalOversendesKlageinstansen().forEach { klagebehandling ->
                    val kontekstTilLog =
                        "sakId: ${klagebehandling.sakId}, saksnummer: ${klagebehandling.saksnummer}, klagebehandlingId: ${klagebehandling.id}"
                    logger.info { "Prøver å oversende til Nav Klageinstans. $kontekstTilLog" }

                    Either.runCatching {
                        val journalpostIdVedtak = klagebehandling.formkrav.vedtakDetKlagesPå!!.let {
                            sak.hentJournalpostIdForVedtakId(it)
                        }
                        // Left skal logges i klienten.
                        kabalClient.oversend(klagebehandling, journalpostIdVedtak).onRight {
                            val oppdatertKlagebehandling =
                                klagebehandling.oppdaterOversendtKlageinstansenTidspunkt(it.oversendtTidspunkt)
                            val statistikkDTO = statistikkService.generer(
                                Statistikkhendelser(
                                    oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.OVERSENDT_KA),
                                ),
                            )
                            sessionFactory.withTransactionContext { tx ->
                                klagebehandlingRepo.markerOversendtTilKlageinstans(oppdatertKlagebehandling, it, tx)
                                statistikkService.lagre(statistikkDTO, tx)
                            }
                        }.onLeft {
                            when (it) {
                                FeilVedOversendelseTilKabal.UkjentFeil -> return@onLeft

                                is FeilVedOversendelseTilKabal.FeilMedResponse -> {
                                    if (it.kanPrøvesIgjen) {
                                        logger.info { "Oversending til klageinstans feilet, men vil prøves igjen ved neste kjøring - $kontekstTilLog" }
                                        return@onLeft
                                    }

                                    logger.error { "Oversending til klageinstans feilet med status ${it.metadata.statusKode} - $kontekstTilLog" }

                                    val oppdatertKlagebehandling =
                                        klagebehandling.oppdaterOversendtKlageinstansFeilet(it.metadata.oversendtTidspunkt)

                                    klagebehandlingRepo.markerOversendtTilKlageinstans(
                                        oppdatertKlagebehandling,
                                        it.metadata,
                                    )
                                }
                            }
                        }
                    }.onFailure { e ->
                        logger.error(e) {
                            "Ukjent feil ved kjøring av oversendKlagerTilKlageinstans jobb. SakId: ${sak.id}, Saksnummer: ${sak.saksnummer}, KlagebehandlingId: ${klagebehandling.id}"
                        }
                    }
                }
            }.onFailure { e ->
                logger.error(e) {
                    "Ukjent feil ved kjøring av oversendKlagerTilKlageinstans jobb. sakId: $sakId"
                }
            }
        }
    }
}
