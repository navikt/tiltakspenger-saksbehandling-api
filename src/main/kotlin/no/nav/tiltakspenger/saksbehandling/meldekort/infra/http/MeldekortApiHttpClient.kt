package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
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
    defaultTimeout: Duration = 1.seconds,
    successStatus: (Int) -> Boolean = { it in 200..299 },
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = defaultTimeout
        this.successStatus = successStatus
        this.authTokenProvider = authTokenProvider
    },
) : MeldekortApiKlient {
    private val sakUrl = URI.create("$baseUrl/saksbehandling/sak")

    override suspend fun sendSak(sak: Sak): Either<HttpKlientError, Unit> {
        val payload = serialize(sak.tilMeldekortApiDTO())
        return httpKlient.post<Unit>(sakUrl, payload).map { }
    }
}
