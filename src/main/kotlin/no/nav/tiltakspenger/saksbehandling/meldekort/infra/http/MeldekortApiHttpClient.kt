package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot meldekort-api, bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-meldekort-api
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Klienten logger bevisst ikke selv: den returnerer [HttpKlientError] uendret, og den bærer all HTTP-kontekst
 * (status, rå request/respons, throwable) via `metadata`.
 * Feilloggingen gjøres én gang av konsumenten ([no.nav.tiltakspenger.saksbehandling.meldekort.service.SendTilMeldekortApiService]),
 * som i tillegg har domenekonteksten.
 */
class MeldekortApiHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 1.seconds,
    timeout: Duration = 1.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : MeldekortApiKlient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            connectTimeout = connectTimeout,
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val sakUrl = URI.create("$baseUrl/saksbehandling/sak")

    override suspend fun sendSak(sak: Sak): Either<HttpKlientError, Unit> {
        val payload = serialize(sak.tilMeldekortApiDTO())
        return httpKlient.postJsonUtenSvar(sakUrl, SerialisertJson(payload)).map { }
    }
}
