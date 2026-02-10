package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.PersonRequestItem
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

// Dokumentasjon: https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
// swagger: https://tilgangsmaskin.intern.nav.no/swagger-ui/index.html
class TilgangsmaskinHttpClient(
    baseUrl: String,
    private val scope: String,
    private val texasClient: TexasClient,
    connectTimeout: Duration = 5.seconds,
    private val timeout: Duration = 10.seconds,
) : TilgangsmaskinClient {
    private val client = HttpClient
        .newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private val tilgangTilPersonUri = URI.create("$baseUrl/api/v1/kjerne")
    private val tilgangTilPersonerUri = URI.create("$baseUrl/api/v1/bulk/obo")

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Tilgangsvurdering {
        val oboToken = texasClient.exchangeToken(
            userToken = saksbehandlerToken,
            audienceTarget = scope,
            identityProvider = IdentityProvider.AZUREAD,
        )
        val request = createPostRequest(fnr.verdi, tilgangTilPersonUri, oboToken.token)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status == 204) {
            return Tilgangsvurdering.Godkjent
        }
        val jsonResponse = httpResponse.body()
        if (status == 403) {
            val avvistTilgangResponse = objectMapper.readValue<AvvistTilgangResponse>(jsonResponse)
            Sikkerlogg.info { "Tilgang avvist: ${avvistTilgangResponse.begrunnelse}. Nav-ident: ${avvistTilgangResponse.navIdent}, fnr: ${avvistTilgangResponse.brukerIdent}, regel: ${avvistTilgangResponse.title}" }
            return avvistTilgangResponse.tilAvvistTilgangsvurdering()
        }
        return Tilgangsvurdering.GenerellFeilMotTilgangsmaskin
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): TilgangBulkResponse {
        val oboToken = texasClient.exchangeToken(
            userToken = saksbehandlerToken,
            audienceTarget = scope,
            identityProvider = IdentityProvider.AZUREAD,
        )
        val personRequestItemListe = fnrs.map { PersonRequestItem(brukerId = it.verdi) }
        val request = createPostRequest(objectMapper.writeValueAsString(personRequestItemListe), tilgangTilPersonerUri, oboToken.token)
        val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = httpResponse.statusCode()
        if (status == 207) {
            val jsonResponse = httpResponse.body()
            return objectMapper.readValue<TilgangBulkResponse>(jsonResponse)
        }
        if (status == 413) {
            throw RuntimeException("Forsøkte å sjekke tilgang for flere enn 1000 identer")
        }
        throw RuntimeException("Noe gikk galt ved sjekk mot tilgangsmaskinen, statuskode $status")
    }

    private fun createPostRequest(
        jsonPayload: String,
        uri: URI,
        token: String,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}
