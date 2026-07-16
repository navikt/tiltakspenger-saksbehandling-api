package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.authFeilUtenKall
import no.nav.tiltakspenger.libs.httpklient.harStatus
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.feil.bodySomJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollFeil
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponseDto
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangPersonRequestDto
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tilgangsmaskinen med støtte for enkel- og bulk-tilgangskontroll.
 *
 * Kildekode: https://github.com/navikt/populasjonstilgangskontroll
 * Dokumentasjon: https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
 * API-spec: https://tilgangsmaskin.intern.nav.no/swagger-ui/index.html (Swagger) og https://tilgangsmaskin.intern.nav.no/v3/api-docs (Spec)
 * Slack: #team-tilgangsmaskinen-værsågod
 * Teamkatalog: https://teamkatalogen.nav.no/tag/Tilgangsmaskinen
 *
 * `403` er et domeneutfall (avvist tilgang med strukturert body), ikke en teknisk feil: den er derfor ikke med i `godta`, men utledes fra feiltypen med [harStatus] og [bodySomJson] (teamkonvensjonen).
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class TilgangsmaskinHttpClient(
    baseUrl: String,
    private val scope: String,
    private val texasClient: TexasClient,
    clock: Clock,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : TilgangsmaskinClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(connectTimeout = connectTimeout, timeout = timeout),
        transport = transport,
    )

    private val log = KotlinLogging.logger {}
    private val tilgangTilPersonUri = URI.create("$baseUrl/api/v1/kjerne")
    private val tilgangTilPersonerUri = URI.create("$baseUrl/api/v1/bulk/obo")

    /**
     * Fnr sendes bevisst som rå, ukvotert tekst — IKKE som JSON-streng, selv om OpenAPI-spec-en sier `application/json` med `{"type": "string"}`.
     * Verifisert i tilgangsmaskinens kildekode (TilgangController): endepunktet er `@RequestBody brukerId: String` uten `consumes`, så Spring binder body-bytes rått via `StringHttpMessageConverter` (som har `text/plain` som native type).
     * En JSON-kvotert body ville gjort anførselstegnene til en del av identverdien og sjekket tilgang mot feil ident.
     * `204 No Content` er endepunktets eneste suksess-status (`@ResponseStatus(NO_CONTENT)`); det finnes ingen 200.
     * `403` er `application/problem+json` (`@ProblemDetailApiResponse`) og er et domeneutfall, ikke en teknisk feil — derfor utledes den fra feilkanalen med [harStatus][no.nav.tiltakspenger.libs.httpklient.harStatus] og [bodySomJson][no.nav.tiltakspenger.libs.httpklient.infra.feil.bodySomJson] i stedet for å stå i statusregelen.
     */
    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<TilgangskontrollFeil, Tilgangsvurdering> = exchangeToken(saksbehandlerToken).flatMap { oboToken ->
        httpKlient.postTekst<Unit>(
            uri = tilgangTilPersonUri,
            tekst = fnr.verdi,
            sensitiv = true,
            bearerToken = oboToken,
            godta = Statusregel.Eksakt(204),
        ).fold(
            ifRight = { Tilgangsvurdering.Godkjent.right() },
            ifLeft = { feil ->
                when {
                    // Avvist tilgang er et domeneutfall som utledes fra feiltypen, ikke en teknisk feil.
                    feil.harStatus(403) && feil is HttpKlientError.UventetStatus ->
                        feil.bodySomJson<AvvistTilgangResponse>()
                            .map { it.tilAvvistTilgangsvurdering() }
                            .onRight(::loggAvvist)

                    else -> feil.left()
                }
            },
        )
    }.mapLeft(::tilTilgangskontrollFeil)

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<TilgangskontrollFeil, Map<Fnr, Boolean>> = exchangeToken(saksbehandlerToken).flatMap { oboToken ->
        httpKlient.postJson<TilgangBulkResponseDto>(
            uri = tilgangTilPersonerUri,
            body = fnrs.map { TilgangPersonRequestDto(brukerId = it.verdi) },
            bearerToken = oboToken,
            godta = Statusregel.Eksakt(207),
        ).map { it.body.tilTilgangPerFnr() }
    }.mapLeft(::tilTilgangskontrollFeil)

    private suspend fun exchangeToken(saksbehandlerToken: String): Either<HttpKlientError, AccessToken> {
        return Either
            .catch {
                texasClient.exchangeToken(
                    userToken = saksbehandlerToken,
                    audienceTarget = scope,
                    identityProvider = IdentityProvider.AZUREAD,
                )
            }
            // OBO-vekslingen feiler før noe HTTP-kall er gjort; authFeilUtenKall gir samme form som klientens egne auth-feil.
            .mapLeft(::authFeilUtenKall)
    }

    private fun tilTilgangskontrollFeil(feil: HttpKlientError): TilgangskontrollFeil =
        if (feil is HttpKlientError.UventetStatus && feil.statusCode == 413) {
            TilgangskontrollFeil.ForMangeIdenter
        } else {
            TilgangskontrollFeil.Uventet(feil)
        }

    private fun loggAvvist(avvist: Tilgangsvurdering.Avvist) {
        log.info { "Tilgang avvist av tilgangsmaskinen. Nav-ident: ${avvist.metadata.navIdent}, regel: ${avvist.metadata.type}, årsak: ${avvist.årsak}. Se sikkerlogg for detaljer." }
        Sikkerlogg.info { "Tilgang avvist: ${avvist.begrunnelse}. Nav-ident: ${avvist.metadata.navIdent}, fnr: ${avvist.metadata.brukerIdent}, regel: ${avvist.metadata.type}, årsak: ${avvist.årsak}." }
    }
}
