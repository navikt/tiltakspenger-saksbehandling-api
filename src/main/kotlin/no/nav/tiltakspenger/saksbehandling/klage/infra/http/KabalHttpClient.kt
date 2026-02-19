package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.infra.http.isSuccess
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.FeilVedOversendelseTilKabal
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock

/**
 * docs: https://github.com/navikt/kabal-api/tree/main/docs/integrasjon#integrere-med-kabal
 * swagger (husk naisdevice): https://kabal-api.intern.dev.nav.no/swagger-ui/index.html?urls.primaryName=external
 */
class KabalHttpClient(
    baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    private val clock: Clock,
) : KabalClient {
    private val client = java.net.http.HttpClient
        .newBuilder()
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val oversendelseUrl = URI.create("$baseUrl/api/oversendelse/v4/sak")

    private val logger = KotlinLogging.logger {}

    override suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<FeilVedOversendelseTilKabal, OversendtKlageTilKabalMetadata> {
        return Either.catch {
            val payload = serialize(klagebehandling.toOversendelsesDto(journalpostIdVedtak))

            val response = client.sendAsync(
                createRequest(oversendelseUrl, payload),
                HttpResponse.BodyHandlers.ofString(),
            ).await()

            if (!response.isSuccess()) {
                logger.error {
                    "Feil ved oversendelse av klagebehandling ${klagebehandling.id} for sak ${klagebehandling.saksnummer} til kabal - " +
                        "status: ${response.statusCode()}, body: ${response.body()}"
                }
                FeilVedOversendelseTilKabal.left()
            } else {
                logger.info { "Klagebehandling ${klagebehandling.id} for sak ${klagebehandling.saksnummer} ble oversendt til kabal" }
                OversendtKlageTilKabalMetadata(
                    request = payload,
                    response = response.body(),
                    oversendtTidspunkt = nå(clock),
                ).right()
            }
        }.mapLeft {
            logger.error(it) {
                "Feil ved oversendelse av klagebehandling ${klagebehandling.id} for sak ${klagebehandling.saksnummer} til kabal"
            }
            FeilVedOversendelseTilKabal
        }.flatten()
    }

    private suspend fun createRequest(
        url: URI,
        payload: String,
    ): HttpRequest {
        return HttpRequest
            .newBuilder()
            .uri(url)
            .header("Authorization", "Bearer ${getToken().token}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()
    }
}
