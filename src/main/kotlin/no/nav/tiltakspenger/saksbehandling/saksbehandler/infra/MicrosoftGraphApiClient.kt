package no.nav.tiltakspenger.saksbehandling.saksbehandler.infra

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.http.toURI
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Header
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.saksbehandler.KanIkkeHenteNavnForNavIdent
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Henter navn på en ansatt fra Microsoft Graph basert på navIdent.
 *
 * Kildekode: ikke aktuelt — eksternt API fra Microsoft
 * Dokumentasjon: https://learn.microsoft.com/en-us/graph/use-the-api
 * API-spec: https://developer.microsoft.com/en-us/graph/graph-explorer (Graph Explorer)
 * Slack: ikke aktuelt (eksternt API)
 * Teamkatalog: ikke aktuelt (eksternt API)
 *
 * Api'et kan testes ut i Graph Explorer.
 * Eksempel request: GET v1.0 https://graph.microsoft.com/v1.0/users?$select=givenName,surname&$filter=onPremisesSamAccountName eq 'din nav ident med apostrof'&$count=true
 * Kan enkelt testes ut ved å se hvilken informasjon du kan hente ut ved å bruke https://graph.microsoft.com/v1.0/me
 */
class MicrosoftGraphApiClient(
    private val baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 2.seconds,
    timeout: Duration = 4.seconds,
    clock: Clock,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : NavIdentClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    override suspend fun hentNavnForNavIdent(navIdent: String): Either<KanIkkeHenteNavnForNavIdent, String> {
        if (navIdent == AUTOMATISK_SAKSBEHANDLER_ID) {
            return "Automatisk saksbehandlet".right()
        }
        return httpKlient.getJson<ListOfMicrosoftGraphResponse>(
            uri = uri(navIdent),
            // ConsistencyLevel kreves av Graph for søk med $count=true, se https://learn.microsoft.com/en-us/graph/aad-advanced-queries
            headere = listOf(Header("ConsistencyLevel", "eventual")),
            godta = Statusregel.Eksakt(200),
        ).mapLeft {
            KanIkkeHenteNavnForNavIdent.KallFeilet(it)
        }.flatMap { response ->
            response.tilNavn()
        }
    }

    private fun HttpKlientResponse<ListOfMicrosoftGraphResponse>.tilNavn(): Either<KanIkkeHenteNavnForNavIdent, String> {
        val brukere = body.value
        val bruker = brukere.singleOrNull()
            ?: return KanIkkeHenteNavnForNavIdent.FantIkkeEntydigBruker(
                antallTreff = brukere.size,
                httpKlientMetadata = metadata,
            ).left()
        val navn = "${bruker.givenName} ${bruker.surname}".trim()
        return if (navn.isBlank()) {
            KanIkkeHenteNavnForNavIdent.NavnetErBlankt(httpKlientMetadata = metadata).left()
        } else {
            navn.right()
        }
    }

    /**
     * Denne oppretter en URI med en URLBuilder for at encodingen skal bli riktig for spesialtegn (apostrof ')
     */
    private fun uri(navIdent: String): URI = graphUri(baseUrl = baseUrl, navIdent = navIdent, brukHttps = Configuration.isNais())
}

/**
 * Bygger Graph-URI-en med en URLBuilder for at encodingen skal bli riktig for spesialtegn (apostrof ').
 * Toppnivåfunksjon slik at begge protokollgrenene kan dekkes av tester uten å styre NAIS-miljødeteksjonen.
 */
internal fun graphUri(baseUrl: String, navIdent: String, brukHttps: Boolean): URI {
    val urlBuilder = URLBuilder().apply {
        protocol = if (brukHttps) URLProtocol.HTTPS else URLProtocol.HTTP
        host = baseUrl
        encodedPath = "/users"
        parameters.append("\$select", "givenName,surname")
        parameters.append("\$filter", "onPremisesSamAccountName eq '$navIdent'")
        parameters.append("\$count", "true")
    }
    return urlBuilder.build().toURI()
}

/**
 * Disse feltene hentes ut som en del av select-query parameter i uri().
 * Kun ment brukt av testene utenfor denne fila.
 */
data class MicrosoftGraphResponse(
    val givenName: String,
    val surname: String,
)

/** Kun ment brukt av testene utenfor denne fila. */
data class ListOfMicrosoftGraphResponse(
    val value: List<MicrosoftGraphResponse>,
)
