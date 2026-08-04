package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KabalClient
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class KabalClientFake(
    private val clock: Clock,
) : KabalClient {
    override suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<HttpKlientError, HttpKlientResponse<Unit>> =
        HttpKlientResponse(
            statusCode = 200,
            body = Unit,
            metadata =
            HttpKlientMetadata(
                method = "POST",
                uri = URI.create("http://kabal.test/api/oversendelse/v4/sak"),
                uriSynlighet = UriSynlighet.VanligLogg,
                tidsgrenser = Tidsgrenser(svar = 30.seconds, oppkobling = 10.seconds),
                rawRequestString = "{}",
                rawResponseString = "",
                requestHeaders = emptyMap(),
                responseHeaders = emptyMap(),
                statusCode = 200,
                attempts = 1,
                attemptDurations = emptyList(),
                totalDuration = Duration.ZERO,
                tidsstempler =
                HttpKlientTidsstempler(
                    authStartet = null,
                    authFullført = null,
                    requestSendt = null,
                    responsMottatt = null,
                ),
            ),
        ).right()
}
