package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.felles.ErrorEveryNLogger
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.ports.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.klage.ports.JournalførKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock
import java.time.LocalDate

class JournalførKlagevedtakService(
    private val journalførKlagevedtaksbrevKlient: JournalførKlagebrevKlient,
    private val klagevedtakRepo: KlagevedtakRepo,
    private val genererKlagebrevKlient: GenererKlagebrevKlient,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val errorEveryNLogger = ErrorEveryNLogger(log, 3)

    /** Ment å kalles fra en jobb - journalfører alle klagevedtak som skal sende brev. */
    suspend fun journalfør() {
        Either.catch {
            klagevedtakRepo.hentKlagevedtakSomSkalJournalføres().forEach { vedtak ->
                val correlationId = CorrelationId.generate()
                log.info { "Journalfører vedtaksbrev for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                Either.catch {
                    val vedtaksdato = LocalDate.now(clock)
                    val pdfOgJson = when (vedtak.resultat) {
                        is Klagebehandlingsresultat.Avvist -> genererKlagebrevKlient.genererAvvisningsvedtak(
                            vedtaksdato = vedtaksdato,
                            tilleggstekst = vedtak.behandling.brevtekst!!,
                            hentBrukersNavn = personService::hentNavn,
                            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                            saksnummer = vedtak.saksnummer,
                            fnr = vedtak.fnr,
                            saksbehandlerNavIdent = vedtak.saksbehandler,
                            forhåndsvisning = false,
                        )

                        is Klagebehandlingsresultat.Omgjør -> throw IllegalStateException("Ugyldig tilstand ved journalføring av klagevedtak. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}")

                        is Klagebehandlingsresultat.Opprettholdt -> TODO("Implementer generering av opprettholdelsesbrev før vi kan journalføre")
                    }.getOrElse { return@forEach }

                    log.info { "Vedtaksbrev generert for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                    val journalpostId = journalførKlagevedtaksbrevKlient.journalførAvvisningsvedtakForKlagevedtak(
                        klagevedtak = vedtak,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    log.info { "Vedtaksbrev journalført for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                    klagevedtakRepo.markerJournalført(vedtak.id, vedtaksdato, pdfOgJson.json, journalpostId, nå(clock))
                    log.info { "Vedtaksbrev markert som journalført for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                    errorEveryNLogger.reset()
                }.onLeft {
                    errorEveryNLogger.log(it) { "Feil ved journalføring av vedtaksbrev for vedtak ${vedtak.id}" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under journalføring av førstegangsvedtak." }
        }
    }
}
