package no.nav.tiltakspenger.saksbehandling.distribusjon.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Responsen fra dokdist ved distribusjon av en journalpost.
 * `bestillingsId` er distribusjons-id-en vi lagrer på vedtaket.
 */
internal data class DokdistResponse(
    val bestillingsId: String,
)

/**
 * Klient mot dokdist (distribusjon av journalførte dokumenter), bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/dokdistfordeling
 * Dokumentasjon: https://confluence.adeo.no/display/BOA/dokdistfordeling og https://confluence.adeo.no/display/BOA/Distribuere+dokument+til+bruker
 * API-spec: https://dokdistfordeling.dev.intern.nav.no/swagger-ui/index.html
 * Slack: #team-dokumentløsninger
 * Teamkatalog: https://teamkatalogen.nav.no/team/f3388fcd-898e-40da-8d02-0bf1e3a79120
 *
 * Klienten logger bevisst ikke selv: den returnerer [HttpKlientError] uendret, og den bærer all HTTP-kontekst (status, rå request/respons, throwable) via `metadata`.
 * Feilloggingen gjøres én gang av konsumenten ([no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering.DistribuerRammevedtaksbrevService]), som i tillegg har domenekonteksten.
 * Mangler noe som burde vært felles for alle konsumenter av httpklient, bør det legges til i libs framfor her.
 *
 * [httpKlient] bygges som default ut fra parametrene over ([clock], [authTokenProvider], [connectTimeout], [defaultTimeout], [successStatus]) slik at hele klientoppsettet kan leses ett sted.
 * Sender man inn en egen [httpKlient] (typisk `HttpKlientFake` i test), **ignoreres** de parametrene som kun brukes til å bygge default-klienten.
 *
 * @param clock Klokke som sendes videre til [HttpKlient].
 * Ignoreres hvis [httpKlient] sendes inn.
 * @param authTokenProvider Henter system-token mot dokdist.
 * Ignoreres hvis [httpKlient] sendes inn.
 * @param connectTimeout Connect-timeout for default-klienten.
 * Ignoreres hvis [httpKlient] sendes inn.
 * @param defaultTimeout Per-request timeout for default-klienten.
 * Ignoreres hvis [httpKlient] sendes inn.
 * @param successStatus Predikat for hvilke HTTP-statuser som regnes som suksess i default-klienten.
 * Ignoreres hvis [httpKlient] sendes inn.
 */
class DokdistHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 1.seconds,
    timeout: Duration = 1.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : Dokumentdistribusjonsklient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val uri = URI.create("$baseUrl/rest/v1/distribuerjournalpost")

    override suspend fun distribuerDokument(
        journalpostId: JournalpostId,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, DistribusjonId> {
        val jsonPayload = journalpostId.toDokdistRequest()
        // 409 er en forventet statuskode ved forsøk på å distribuere samme dokument flere ganger, og bodyen har samme form som ved 200 — derfor er den med i godta i stedet for å utledes fra feiltypen.
        return httpKlient.postJson<DokdistResponse>(
            uri = uri,
            body = SerialisertJson(jsonPayload),
            // Dokdist bruker «Nav-Callid»-varianten (uten bindestrek før Id); navCallId ville gitt det andre headernavnet «Nav-Call-Id».
            headere = listOf(NavHeadere.navCallid(correlationId.value)),
            godta = Statusregel.Eksakt(200, 409),
        ).map { DistribusjonId(it.body.bestillingsId) }
    }
}
