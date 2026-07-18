package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot Kabal for oversendelse av klager til klageinstansen.
 *
 * Kildekode: https://github.com/navikt/kabal-api
 * Dokumentasjon: https://github.com/navikt/kabal-api/tree/main/docs/integrasjon#integrere-med-kabal
 * API-spec: https://kabal-api.intern.dev.nav.no/swagger-ui/index.html (swagger) og https://kabal-api.intern.dev.nav.no/v3/api-docs/external (Spec)
 * Slack: #team-klage-værsågod
 * Teamkatalog: https://teamkatalogen.nav.no/team/ba63bde4-19b0-4700-9400-b699d5db76cd
 *
 * Klienten er bevisst minimal: den gjør selve HTTP-kallet, og responsen brukes kun via metadataen (rå request/respons persisteres i `OversendtKlageTilKabalMetadata`).
 * Feillogging og hva som skal skje ved en avvist/feilet oversendelse ligger i [no.nav.tiltakspenger.saksbehandling.klage.infra.jobb.OversendKlageTilKlageinstansJobb], sammen med resten av domenekonteksten.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class KabalHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : KabalClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val oversendelseUrl = URI.create("$baseUrl/api/oversendelse/v4/sak")

    override suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<HttpKlientError, HttpKlientResponse<Unit>> {
        // Serialiseres eksplisitt fordi nøyaktig payload persisteres sammen med responsen i OversendtKlageTilKabalMetadata.
        val payload = serialize(klagebehandling.toOversendelsesDto(journalpostIdVedtak))
        return httpKlient.postJsonUtenSvar(oversendelseUrl, SerialisertJson(payload))
    }
}
