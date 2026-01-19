package no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilRammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.felles.ErrorEveryNLogger
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock
import java.time.LocalDate

class JournalførRammevedtakService(
    private val journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient,
    private val rammevedtakRepo: RammevedtakRepo,
    private val genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient,
    private val genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient,
    private val genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val errorEveryNLogger = ErrorEveryNLogger(log, 3)

    /** Ment å kalles fra en jobb - journalfører alle rammevedtak som skal sende brev. */
    suspend fun journalfør() {
        Either.catch {
            rammevedtakRepo.hentRammevedtakSomSkalJournalføres().forEach { vedtak ->
                val correlationId = CorrelationId.generate()
                log.info { "Journalfører vedtaksbrev for vedtak ${vedtak.id}, type: ${vedtak.resultat.tilRammebehandlingResultatTypeDTO()}" }
                Either.catch {
                    val vedtaksdato = LocalDate.now(clock)
                    val pdfOgJson = when (vedtak.resultat) {
                        is BehandlingResultat.Innvilgelse -> genererVedtaksbrevForInnvilgelseKlient.genererInnvilgetVedtakBrev(
                            vedtaksdato = vedtaksdato,
                            vedtak = vedtak,
                            tilleggstekst = vedtak.behandling.fritekstTilVedtaksbrev,
                            hentBrukersNavn = personService::hentNavn,
                            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                        )

                        is RevurderingResultat.Stans -> genererVedtaksbrevForStansKlient.genererStansvedtak(
                            vedtaksdato = vedtaksdato,
                            vedtak = vedtak,
                            hentBrukersNavn = personService::hentNavn,
                            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                        )

                        is SøknadsbehandlingResultat.Avslag -> genererVedtaksbrevForAvslagKlient.genererAvslagsvVedtaksbrev(
                            vedtak = vedtak,
                            datoForUtsending = vedtaksdato,
                            hentBrukersNavn = personService::hentNavn,
                            hentSaksbehandlersNavn = navIdentClient::hentNavnForNavIdent,
                        )
                    }.getOrElse { return@forEach }

                    log.info { "Vedtaksbrev generert for vedtak ${vedtak.id}" }
                    val journalpostId =
                        journalførRammevedtaksbrevKlient.journalførVedtaksbrevForRammevedtak(
                            vedtak,
                            pdfOgJson,
                            correlationId,
                        )
                    log.info { "Vedtaksbrev journalført for vedtak ${vedtak.id}" }
                    rammevedtakRepo.markerJournalført(vedtak.id, vedtaksdato, pdfOgJson.json, journalpostId, nå(clock))
                    log.info { "Vedtaksbrev markert som journalført for vedtak ${vedtak.id}" }
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
