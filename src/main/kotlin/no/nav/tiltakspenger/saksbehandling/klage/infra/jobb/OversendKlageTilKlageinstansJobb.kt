package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentJournalpostIdForVedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandlingerSomSkalOversendesKlageinstansen
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansenTidspunkt
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OversendKlageTilKlageinstansJobb(
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val sakRepo: SakRepo,
    private val kabalClient: KabalClient,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Oversender alle klagebehandlinger med status [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT] til klageinstansen.
     * Oppdaterer status og tidspunkt.
     */
    suspend fun oversendKlagerTilKlageinstans() {
        klagebehandlingRepo.hentSakerSomSkalOversendesKlageinstansen().forEach { sakId ->
            Either.runCatching {
                val sak: Sak = sakRepo.hentForSakId(sakId)!!
                sak.hentKlagebehandlingerSomSkalOversendesKlageinstansen().forEach { klagebehandling ->
                    Either.runCatching {
                        val journalpostIdVedtak = klagebehandling.formkrav.vedtakDetKlagesPå!!.let {
                            sak.hentJournalpostIdForVedtakId(it)
                        }
                        // Left skal logges i klienten.
                        kabalClient.oversend(klagebehandling, journalpostIdVedtak).onRight {
                            klagebehandlingRepo.markerOversendtTilKlageinstans(
                                klagebehandling = klagebehandling.oppdaterOversendtKlageinstansenTidspunkt(it.oversendtTidspunkt),
                                metadata = it,
                            )
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
