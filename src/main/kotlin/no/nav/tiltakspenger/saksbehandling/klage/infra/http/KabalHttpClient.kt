package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata
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
 * Klienten er bevisst minimal: den gjør selve HTTP-kallet og mapper en vellykket respons til [OversendtKlageTilKabalMetadata].
 * Feillogging og hva som skal skje ved en avvist/feilet oversendelse ligger i [no.nav.tiltakspenger.saksbehandling.klage.infra.jobb.OversendKlageTilKlageinstansJobb], sammen med resten av domenekonteksten.
 */
class KabalHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    defaultTimeout: Duration = 10.seconds,
    successStatus: (Int) -> Boolean = { it in 200..299 },
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = defaultTimeout
        this.successStatus = successStatus
        this.authTokenProvider = authTokenProvider
    },
) : KabalClient {
    private val oversendelseUrl = URI.create("$baseUrl/api/oversendelse/v4/sak")

    override suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<HttpKlientError, HttpKlientResponse<String>> {
        val payload = serialize(klagebehandling.toOversendelsesDto(journalpostIdVedtak))
        return httpKlient.post<String>(oversendelseUrl) {
            json(payload)
        }
    }
}
