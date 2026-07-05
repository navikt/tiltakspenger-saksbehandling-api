package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import java.time.Clock
import kotlin.time.Duration

class KabalClientFake(
    private val clock: Clock,
) : KabalClient {
    override suspend fun oversend(
        klagebehandling: Klagebehandling,
        journalpostIdVedtak: JournalpostId,
    ): Either<HttpKlientError, HttpKlientResponse<String>> =
        HttpKlientResponse(
            statusCode = 200,
            body = "",
            metadata =
            HttpKlientMetadata(
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
