package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.harStatus
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.feil.bodySomJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Header
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalføring.KunneIkkeJournalføre
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.toJournalpostRequest
import no.nav.tiltakspenger.saksbehandling.klage.ports.JournalførKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.toJournalpostRequest
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.http.utgåendeJournalpostRequest
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot dokarkiv for å opprette journalposter (journalføring av brev).
 *
 * Kildekode: https://github.com/navikt/dokarkiv
 * Dokumentasjon: https://confluence.adeo.no/display/BOA/dokarkiv og https://confluence.adeo.no/display/BOA/opprettJournalpost
 * API-spec: https://dokarkiv.dev.intern.nav.no/swagger-ui/index.html
 * Slack: #team-dokumentløsninger
 * Teamkatalog: https://teamkatalogen.nav.no/team/f3388fcd-898e-40da-8d02-0bf1e3a79120
 *
 * `409 Conflict` er et domeneutfall (journalposten er allerede opprettet, dedup på eksternReferanseId) med samme bodyform som `201`: den utledes fra feiltypen med [harStatus]/[bodySomJson] i stedet for å stå i statusregelen.
 * Dedupen er også grunnen til at [Retry.Fast.retryIkkeIdempotente] er trygt her: et nytt forsøk etter et uvisst utfall gir i verste fall en `409` som behandles som suksess.
 *
 * Klienten logger ikke feil selv: den bærer konteksten videre via [KunneIkkeJournalføre], og feillogging gjøres én gang i kallende jobb via [no.nav.tiltakspenger.saksbehandling.journalføring.loggFeil].
 * Unntaket er én error-linje når dokarkiv oppretter journalposten uten å ferdigstille den — kallet er da en suksess (ingen Left å logge hos kalleren), men tilstanden krever manuell oppfølging.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
internal class DokarkivHttpClient(
    baseUrl: String,
    private val clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 30.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : JournalførRammevedtaksbrevKlient,
    JournalførMeldekortKlient,
    JournalførKlagebrevKlient {

    private val log = KotlinLogging.logger {}

    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            // Paritet med den gamle ktor-klienten (`httpClientWithRetry`): fire forsøk totalt med konstant 100 ms delay.
            retry = Retry.Fast(maksForsøk = 4, delay = 100.milliseconds, retryIkkeIdempotente = true),
        ),
        transport = transport,
    )

    private val opprettJournalpostUri = URI.create("$baseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true")

    override suspend fun journalførVedtaksbrevForRammevedtak(
        vedtak: Rammevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        return opprettJournalpost({ vedtak.utgåendeJournalpostRequest(pdfOgJson) }, correlationId)
    }

    override suspend fun journalførVedtaksbrevForMeldekortvedtak(
        meldekortvedtak: Meldekortvedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        return opprettJournalpost({ meldekortvedtak.toJournalpostRequest(pdfOgJson) }, correlationId)
    }

    override suspend fun journalførAvvisningsvedtakForKlagevedtak(
        klagevedtak: Klagevedtak,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        return opprettJournalpost({ klagevedtak.toJournalpostRequest(pdfOgJson) }, correlationId)
    }

    override suspend fun journalførInnstillingsbrevForOpprettholdtKlagebehandling(
        klagebehandling: Klagebehandling,
        pdfOgJson: PdfOgJson,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        return opprettJournalpost({ klagebehandling.toJournalpostRequest(pdfOgJson) }, correlationId)
    }

    /**
     * [byggRequest] kjøres bak [Either.catch] slik at feil i domenet → JSON-mappingen (manglende påkrevde verdier, serialisering) blir en typet Left i stedet for en exception som river ned jobben.
     */
    private suspend fun opprettJournalpost(
        byggRequest: () -> String,
        correlationId: CorrelationId,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        val jsonRequestBody = Either.catch { byggRequest() }
            .getOrElse { return KunneIkkeJournalføre.KunneIkkeByggeRequest(it).left() }

        return httpKlient.postJson<DokarkivResponse>(
            uri = opprettJournalpostUri,
            body = SerialisertJson(jsonRequestBody),
            headere = listOf(
                Header("X-Correlation-ID", correlationId.value),
                // Dokarkiv bruker «Nav-Callid»-varianten (uten bindestrek før Id); navCallId ville gitt det andre headernavnet «Nav-Call-Id».
                NavHeadere.navCallid(correlationId.value),
            ),
            godta = Statusregel.Eksakt(201),
        ).fold(
            ifRight = { respons ->
                respons.body.tilJournalførteDokumenter(
                    statusCode = respons.statusCode,
                    requestBody = jsonRequestBody,
                    metadata = respons.metadata,
                )
            },
            ifLeft = { feil ->
                when {
                    // Allerede journalført (dedup): samme bodyform som 201, utledes fra feiltypen.
                    feil.harStatus(409) && feil is HttpKlientError.UventetStatus ->
                        feil.bodySomJson<DokarkivResponse>()
                            .mapLeft { KunneIkkeJournalføre.KallFeilet(it) }
                            .flatMap { respons ->
                                respons.tilJournalførteDokumenter(
                                    statusCode = feil.statusCode,
                                    requestBody = jsonRequestBody,
                                    metadata = feil.metadata,
                                )
                            }

                    else -> KunneIkkeJournalføre.KallFeilet(feil).left()
                }
            },
        )
    }

    private fun DokarkivResponse.tilJournalførteDokumenter(
        statusCode: Int,
        requestBody: String,
        metadata: HttpKlientMetadata,
    ): Either<KunneIkkeJournalføre, JournalførteDokumenter> {
        val journalpostId = journalpostId?.takeIf { it.isNotEmpty() }
            ?: return KunneIkkeJournalføre.UgyldigRespons(
                begrunnelse = "Dokarkiv svarte ${statusCode.tilResponseStatusTekst()} uten journalpostId",
                metadata = metadata,
            ).left()

        if (statusCode == 201 && journalpostferdigstilt != true) {
            // Suksess uten Left, så kalleren har ingen feil å logge — dette driftssignalet (manuell ferdigstilling i Gosys) logges derfor her.
            log.error { "Dokarkiv opprettet journalpost $journalpostId uten å ferdigstille den. Se sikkerlogg for responsen." }
            Sikkerlogg.error { "Dokarkiv opprettet journalpost $journalpostId uten å ferdigstille den. response: ${metadata.rawResponseString}. request: ${metadata.rawRequestString}" }
        }

        return JournalførteDokumenter(
            journalpostId = JournalpostId(journalpostId),
            dokumentInfoIder = dokumentInfoIder(),
            metadata = JournalførBrevMetadata(
                requestBody = requestBody,
                responseStatus = statusCode.tilResponseStatusTekst(),
                responseBody = metadata.rawResponseString.orEmpty(),
                journalføringsTidspunkt = metadata.tidsstempler.responsMottatt ?: nå(clock),
            ),
        ).right()
    }

    data class DokarkivResponse(
        val journalpostId: String?,
        val journalpostferdigstilt: Boolean?,
        val melding: String?,
        val dokumenter: List<Dokumenter>?,
    ) {
        fun dokumentInfoIder(): NonEmptyList<DokumentInfoId>? = dokumenter?.mapNotNull {
            it.dokumentInfoId?.let { DokumentInfoId(it) }
        }?.toNonEmptyListOrNull()
    }

    data class Dokumenter(
        val dokumentInfoId: String?,
    )
}

/**
 * Notoritetsformatet i [JournalførBrevMetadata.responseStatus], som persisteres og må matche det den gamle klienten skrev («201 Created»/«409 Conflict»).
 * Kun disse to statusene kan nå hit (statusregelen godtar bare `201`, og feilgrenen slipper bare gjennom `409`); alt annet er en programmeringsfeil i statushåndteringen og feiler høylytt.
 * Toppnivåfunksjon slik at også feilgrenen kan dekkes av tester.
 */
internal fun Int.tilResponseStatusTekst(): String = when (this) {
    201 -> "201 Created"
    409 -> "409 Conflict"
    else -> throw IllegalStateException("Uventet statuskode $this ved journalføring mot dokarkiv - kun 201 og 409 skal kunne nå hit.")
}
