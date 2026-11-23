package no.nav.tiltakspenger.saksbehandling.saksbehandler.infra

import arrow.core.Either
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.toURI
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.saksbehandling.saksbehandler.EntraGruppe
import no.nav.tiltakspenger.saksbehandling.saksbehandler.EntraProxyClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.KanIkkeHenteGrupper
import java.net.URI
import java.time.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot entra-proxy, som slår opp ansattdata i Entra ID, NORG og NOM.
 *
 * Kildekode: https://github.com/navikt/entra-proxy
 * Dokumentasjon: https://confluence.adeo.no/spaces/TM/pages/758383588/entra-proxy
 * API-spec: https://entraproxy.intern.nav.no/swagger-ui/index.html (Swagger)
 * Slack: #team-sikkerhetstjenesten
 * Teamkatalog: ikke funnet (Team Sikkerhetstjenesten)
 *
 * Kallene gjøres med systemtoken (client credentials), siden oppslaget gjelder en vilkårlig ansatt og ikke den innloggede saksbehandleren.
 * Navidenten ligger i stien, så URI-en logges bare til sikkerlogg (defaulten i [HttpKlientConfig]).
 * Klienten logger aldri, og gruppedata i feilene den returnerer går bare til sikkerlogg; se [utenGruppedataIVanligLogg].
 */
class EntraProxyHttpClient(
    private val baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    clock: Clock,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : EntraProxyClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    /**
     * Entra-proxy filtrerer på `securityEnabled eq true and mailEnabled eq false` og bruker `memberOf`, ikke `transitiveMemberOf`.
     * En ident uten treff i Entra gir `200` med tom liste, ikke `404`.
     */
    override suspend fun hentGrupper(navIdent: String): Either<KanIkkeHenteGrupper, Set<EntraGruppe>> =
        httpKlient.getJson<List<EntraGruppeDto>>(
            uri = grupperUri(navIdent),
            godta = Statusregel.Eksakt(200),
        )
            .map { response -> response.body.mapTo(mutableSetOf()) { it.tilDomene() } }
            .mapLeft { KanIkkeHenteGrupper.KallFeilet(it.utenGruppedataIVanligLogg()) }

    /**
     * Gruppemedlemskap kan avsløre at en ident er fiktiv, og skal derfor aldri havne i vanlig logg.
     * Svaret i `body` og `rawResponseString` går bare til sikkerlogg via `loggFeil`, og beholdes for feilsøking.
     * Exceptionen fra Jackson går derimot til vanlig logg som `cause`, og meldingen dens siterer verdier fra svaret.
     * Den byttes derfor ut med en som bare navngir exception-typen.
     */
    private fun HttpKlientError.utenGruppedataIVanligLogg(): HttpKlientError =
        when (this) {
            is HttpKlientError.DeserializationError -> copy(
                throwable = IllegalStateException(
                    "${throwable::class.simpleName} ved deserialisering av svar fra entra-proxy. " +
                        "Meldingen er fjernet fordi den kan inneholde gruppedata; se responsen i sikkerlogg.",
                ),
            )

            else -> this
        }

    /**
     * Identen kommer utenfra, så den legges inn som ett stisegment der også `/` encodes.
     * Uten `encodeSlash` ville en ident som `Z12/../x` endret stien.
     */
    private fun grupperUri(navIdent: String): URI =
        URLBuilder(baseUrl)
            .appendPathSegments(listOf("api", "v1", "ansatt", "tilganger", navIdent), encodeSlash = true)
            .build()
            .toURI()
}

/**
 * Feltnavnene følger `EntraGruppe` i entra-proxy, der `rolle` er gruppens visningsnavn.
 * `gruppeId` er nullbar i entra-proxy sin modell, men alltid satt for gruppene fra `memberOf`; mangler den, feiler deserialiseringen.
 * Kun ment brukt av testene utenfor denne fila.
 */
data class EntraGruppeDto(
    val rolle: String,
    val gruppeId: UUID,
) {
    fun tilDomene(): EntraGruppe = EntraGruppe(id = gruppeId, navn = rolle)
}
