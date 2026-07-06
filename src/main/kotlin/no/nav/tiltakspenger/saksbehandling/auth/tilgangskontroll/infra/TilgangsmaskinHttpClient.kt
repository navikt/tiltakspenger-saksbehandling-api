package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.post
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
 * Dokumentasjon: https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen
 * Swagger: https://tilgangsmaskin.intern.nav.no/swagger-ui/index.html
 */
class TilgangsmaskinHttpClient(
    baseUrl: String,
    private val scope: String,
    private val texasClient: TexasClient,
    clock: Clock,
    connectTimeout: Duration = 5.seconds,
    defaultTimeout: Duration = 10.seconds,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = defaultTimeout
    },
) : TilgangsmaskinClient {
    private val log = KotlinLogging.logger {}
    private val tilgangTilPersonUri = URI.create("$baseUrl/api/v1/kjerne")
    private val tilgangTilPersonerUri = URI.create("$baseUrl/api/v1/bulk/obo")

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<TilgangskontrollFeil, Tilgangsvurdering> = exchangeToken(saksbehandlerToken).flatMap { oboToken ->
        httpKlient.post<String>(tilgangTilPersonUri) {
            body(fnr.verdi)
            bearerToken(oboToken)
            successStatus(204, 403)
        }.flatMap { response ->
            // successStatus(204, 403) garanterer at kun disse to statusene når hit; alt annet er allerede en Left(UventetStatus).
            if (response.statusCode == 204) {
                Tilgangsvurdering.Godkjent.right()
            } else {
                AvvistTilgangResponse.tilAvvistTilgangsvurdering(response.body, response.statusCode, response.metadata)
                    .onRight(::loggAvvist)
            }
        }
    }.mapLeft(::tilTilgangskontrollFeil)

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<TilgangskontrollFeil, Map<Fnr, Boolean>> = exchangeToken(saksbehandlerToken).flatMap { oboToken ->
        httpKlient.post<TilgangBulkResponseDto>(tilgangTilPersonerUri) {
            json(fnrs.map { TilgangPersonRequestDto(brukerId = it.verdi) })
            bearerToken(oboToken)
            successStatus(207)
        }.map { it.body.tilTilgangPerFnr() }
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
            .mapLeft(::tilAuthError)
    }

    private fun tilAuthError(throwable: Throwable): HttpKlientError = HttpKlientError.AuthError(
        throwable = throwable,
        metadata = HttpKlientMetadata(
            rawRequestString = "",
            rawResponseString = null,
            requestHeaders = emptyMap(),
            responseHeaders = emptyMap(),
            statusCode = null,
            attempts = 0,
            attemptDurations = emptyList(),
            totalDuration = Duration.ZERO,
            tidsstempler = HttpKlientTidsstempler.INGEN,
        ),
    )

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
