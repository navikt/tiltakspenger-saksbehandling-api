package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentJournalpostIdForVedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandlingerSomSkalOversendesKlageinstansen
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansenTidspunkt
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class OversendKlageTilKlageinstansJobb(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val sakService: SakService,
    private val kabalClient: KabalClient,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
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
                            val statistikk =
                                statistikkSakService.genererSaksstatistikkForKlagebehandlingOversendtTilKabal(
                                    klagebehandling,
                                )

                            sessionFactory.withTransactionContext { tx ->
                                klagebehandlingRepo.markerOversendtTilKlageinstans(
                                    klagebehandling = klagebehandling.oppdaterOversendtKlageinstansenTidspunkt(it.oversendtTidspunkt),
                                    metadata = it,
                                )
                                statistikkSakRepo.lagre(statistikk, tx)
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
