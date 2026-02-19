package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

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
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock
import java.time.LocalDate

class JournalførKlagebrevJobb(
    private val journalførKlagevedtaksbrevKlient: JournalførKlagebrevKlient,
    private val klagevedtakRepo: KlagevedtakRepo,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val genererKlagebrevKlient: GenererKlagebrevKlient,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val errorEveryNLogger = ErrorEveryNLogger(log, 3)

    suspend fun journalførAvvisningbrev() {
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

                        is Klagebehandlingsresultat.Omgjør, is Klagebehandlingsresultat.Opprettholdt -> throw IllegalStateException(
                            "Ugyldig resultat (${vedtak.resultat::class.simpleName} ved journalføring av klagevedtak. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}",
                        )
                    }.getOrElse { return@forEach }

                    log.info { "Vedtaksbrev generert for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                    val (journalpostId, metadata) = journalførKlagevedtaksbrevKlient.journalførAvvisningsvedtakForKlagevedtak(
                        klagevedtak = vedtak,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    log.info { "Vedtaksbrev journalført for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                    klagevedtakRepo.markerJournalført(
                        vedtak.id,
                        vedtaksdato,
                        metadata,
                        journalpostId,
                        nå(clock),
                    )
                    log.info { "Vedtaksbrev markert som journalført for klagevedtak ${vedtak.id}, type: ${vedtak.resultat}. sakId: ${vedtak.sakId}, saksnummer: ${vedtak.saksnummer}" }
                    errorEveryNLogger.reset()
                }.onLeft {
                    errorEveryNLogger.log(it) { "Feil ved journalføring av vedtaksbrev for vedtak ${vedtak.id}" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under journalføring av klage avvisningsbrev." }
        }
    }

    suspend fun journalførInnstillingsbrev() {
        Either.catch {
            klagebehandlingRepo.hentInnstillingsbrevSomSkalJournalføres().forEach { klagebehandling ->
                val sakId = klagebehandling.sakId
                val saksnummer = klagebehandling.saksnummer
                val id = klagebehandling.id
                val loggkontekst = "sakId: $sakId, saksnummer: $saksnummer, klagebehandlingId: $id"
                Either.catch {
                    val correlationId = CorrelationId.generate()
                    val vårDatoIBrevet = LocalDate.now(clock)
                    log.info { "Genererer innstillingsbrev. $loggkontekst" }
                    val pdfOgJson = genererKlagebrevKlient.genererInnstillingsbrev(
                        saksnummer = saksnummer,
                        fnr = klagebehandling.fnr,
                        tilleggstekst = klagebehandling.brevtekst!!,
                        saksbehandlerNavIdent = klagebehandling.saksbehandler!!,
                        forhåndsvisning = false,
                        vedtaksdato = vårDatoIBrevet,
                        hentBrukersNavn = personService::hentNavn,
                        hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                        innsendingsdato = klagebehandling.formkrav.innsendingsdato,
                    ).getOrElse { return@forEach }

                    log.info { "Innstillingsbrev generert. $loggkontekst" }
                    val (journalpostId, metadata) = journalførKlagevedtaksbrevKlient.journalførInnstillingsbrevForOpprettholdtKlagebehandling(
                        klagebehandling = klagebehandling,
                        pdfOgJson = pdfOgJson,
                        correlationId = correlationId,
                    )
                    val oppdatertKlagebehandling = klagebehandling.oppdaterInnstillingsbrevJournalpost(
                        brevdato = vårDatoIBrevet,
                        journalpostId = journalpostId,
                        tidspunkt = metadata.journalføringsTidspunkt,
                    )
                    log.info { "Innstillingsbrev journalført. Prøver å lagre. journalpostId: $journalpostId, $loggkontekst" }
                    klagebehandlingRepo.markerInnstillingsbrevJournalført(oppdatertKlagebehandling, metadata)
                    log.info { "Innstillingsbrev markert som journalført. journalpostId: $journalpostId, $loggkontekst" }
                    errorEveryNLogger.reset()
                }.onLeft {
                    errorEveryNLogger.log(it) { "Feil ved journalføring av innstillingsbrev. $loggkontekst" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under journalføring av innstillingsbrev." }
        }
    }
}
