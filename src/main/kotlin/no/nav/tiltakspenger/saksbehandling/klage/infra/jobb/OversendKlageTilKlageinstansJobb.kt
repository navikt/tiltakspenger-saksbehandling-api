package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.http.loggFeil
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentJournalpostIdForVedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandlingerSomSkalOversendesKlageinstansen
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansFeilet
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansenTidspunkt
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.tilOversendtKlageTilKabalMetadata
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

class OversendKlageTilKlageinstansJobb(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val sakService: SakService,
    private val kabalClient: KabalClient,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
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
                        kabalClient.oversend(klagebehandling, journalpostIdVedtak).onRight {
                            val metadata = it.metadata.tilOversendtKlageTilKabalMetadata(clock = clock)
                            val oppdatertKlagebehandling =
                                klagebehandling.oppdaterOversendtKlageinstansenTidspunkt(metadata.oversendtTidspunkt)
                            val statistikkDTO = statistikkService.generer(
                                Statistikkhendelser(
                                    oppdatertKlagebehandling.genererSaksstatistikk(StatistikkhendelseType.OVERSENDT_KA),
                                ),
                            )
                            sessionFactory.withTransactionContext { tx ->
                                klagebehandlingRepo.markerOversendtTilKlageinstans(
                                    klagebehandling = oppdatertKlagebehandling,
                                    metadata = metadata,
                                    sessionContext = tx,
                                )
                                statistikkService.lagre(statistikkDTO, tx)
                            }
                        }.onLeft { error ->
                            error.loggFeil(logger, "oversendelse av klage til klageinstans", kontekstTilLog)

                            if (error is HttpKlientError.ResponsMottatt && error.statusCode == 400) {
                                // Kabal avviste klagen (bad request) — terminalt, klagen forsøkes ikke igjen.
                                val metadata = error.tilOversendtKlageTilKabalMetadata(clock = clock)
                                val oppdatertKlagebehandling =
                                    klagebehandling.oppdaterOversendtKlageinstansFeilet(metadata.oversendtTidspunkt)
                                klagebehandlingRepo.markerOversendtTilKlageinstans(oppdatertKlagebehandling, metadata)
                            }
                            // Andre feil: klagen står igjen og forsøkes på nytt ved neste kjøring.
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
