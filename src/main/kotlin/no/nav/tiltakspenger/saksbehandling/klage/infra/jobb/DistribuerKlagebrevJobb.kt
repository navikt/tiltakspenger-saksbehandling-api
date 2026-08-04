package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.Clock

class DistribuerKlagebrevJobb(
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val klagevedtakRepo: KlagevedtakRepo,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}

    suspend fun distribuerAvvisningsbrev() {
        Either.catch {
            klagevedtakRepo.hentKlagevedtakSomSkalDistribueres().forEach { vedtakSomSkalDistribueres ->
                distribuerAvvisningsbrev(vedtakSomSkalDistribueres)
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av klagevedtaksbrev." }
        }
    }

    suspend fun distribuerAvvisningsbrevForSak(sakId: SakId) {
        Either.catch {
            klagevedtakRepo.hentKlagevedtakSomSkalDistribueresForSakId(sakId).forEach { vedtakSomSkalDistribueres ->
                distribuerAvvisningsbrev(vedtakSomSkalDistribueres)
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av klagevedtaksbrev for sak $sakId." }
        }
    }

    private suspend fun distribuerAvvisningsbrev(vedtakSomSkalDistribueres: VedtakSomSkalDistribueres) {
        val correlationId = CorrelationId.generate()
        log.info { "Prøver å distribuere journalpost for avvist klagevedtaksbrev. $vedtakSomSkalDistribueres" }
        Either.catch {
            val distribusjonId =
                dokumentdistribusjonsklient.distribuerDokument(
                    vedtakSomSkalDistribueres.journalpostId,
                    correlationId,
                )
                    .getOrElse {
                        log.error { "Kunne ikke distribuere avvist klagevedtaksbrev. $vedtakSomSkalDistribueres" }
                        return
                    }
            log.info { "Avvist klagevedtaksbrev distribuert. $vedtakSomSkalDistribueres" }
            klagevedtakRepo.markerDistribuert(vedtakSomSkalDistribueres.id, distribusjonId, nå(clock))
            log.info { "Avvist klagevedtaksbrev markert som distribuert. distribusjonId: $distribusjonId, $vedtakSomSkalDistribueres" }
        }.onLeft {
            log.error(it) { "Feil ved distribuering av avvist klagevedtaksbrev. $vedtakSomSkalDistribueres" }
        }
    }

    suspend fun distribuerInnstillingsbrev() {
        Either.catch {
            klagebehandlingRepo.hentInnstillingsbrevSomSkalDistribueres().forEach { behandlingSomSkalDistribueres ->
                distribuerInnstillingsbrev(behandlingSomSkalDistribueres)
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av innstillingsbrev." }
        }
    }

    suspend fun distribuerInnstillingsbrev(klagebehandlingId: KlagebehandlingId) {
        Either.catch {
            val klagebehandling = klagebehandlingRepo.hentForKlagebehandlingId(klagebehandlingId) ?: return
            val resultat = klagebehandling.resultat
            if (klagebehandling.status != Klagebehandlingsstatus.OPPRETTHOLDT ||
                resultat !is Klagebehandlingsresultat.Opprettholdt ||
                resultat.journalpostIdInnstillingsbrev == null ||
                resultat.distribusjonIdInnstillingsbrev != null
            ) {
                return
            }
            distribuerInnstillingsbrev(klagebehandling)
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under distribuering av innstillingsbrev for klagebehandling $klagebehandlingId." }
        }
    }

    private suspend fun distribuerInnstillingsbrev(behandlingSomSkalDistribueres: Klagebehandling) {
        val correlationId = CorrelationId.generate()
        val journalpostId: JournalpostId =
            (behandlingSomSkalDistribueres.resultat as Klagebehandlingsresultat.Opprettholdt).journalpostIdInnstillingsbrev!!
        val kontekstTilLog =
            "sakId: ${behandlingSomSkalDistribueres.sakId}, saksnummer: ${behandlingSomSkalDistribueres.saksnummer}, klagebehandlingId: ${behandlingSomSkalDistribueres.id}, journalpostId: $journalpostId"
        log.info { "Prøver å distribuere innstillingsbrev. $kontekstTilLog" }
        Either.catch {
            val distribusjonId =
                dokumentdistribusjonsklient.distribuerDokument(journalpostId, correlationId).getOrElse {
                    log.error { "Kunne ikke distribuere innstillingsbrev. Underliggende feil: $it. $kontekstTilLog" }
                    return
                }
            log.info { "Innstillingsbrev distribuert. distribusjonId: $distribusjonId, $kontekstTilLog" }
            klagebehandlingRepo.lagreKlagebehandling(
                klagebehandling = behandlingSomSkalDistribueres.oppdaterInnstillingsbrevDistribusjon(
                    distribusjonId = distribusjonId,
                    tidspunkt = nå(clock),
                ),
            )
            log.info { "Innstillingsbrev markert som distribuert. distribusjonId: $distribusjonId, $kontekstTilLog" }
        }.onLeft {
            log.error(it) { "Feil ved distribuering av innstillingsbrev. $kontekstTilLog" }
        }
    }
}
