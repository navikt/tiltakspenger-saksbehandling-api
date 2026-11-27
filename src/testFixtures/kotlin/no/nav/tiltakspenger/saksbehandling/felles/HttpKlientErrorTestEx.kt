package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Et 500-svar fra en ekstern tjeneste, med [rawRequestString] som requesten klienten sendte. */
fun uventetStatus(
    uri: String = "http://ekstern.test/api",
    rawRequestString: String = "",
): HttpKlientError.UventetStatus = HttpKlientError.UventetStatus(
    statusCode = 500,
    body = "",
    metadata = HttpKlientMetadata(
        method = "POST",
        uri = URI.create(uri),
        uriSynlighet = UriSynlighet.VanligLogg,
        tidsgrenser = Tidsgrenser(svar = 5.seconds, oppkobling = 3.seconds),
        rawRequestString = rawRequestString,
        rawResponseString = null,
        requestHeaders = emptyMap(),
        responseHeaders = emptyMap(),
        statusCode = 500,
        attempts = 1,
        attemptDurations = emptyList(),
        totalDuration = Duration.ZERO,
        tidsstempler = HttpKlientTidsstempler.INGEN,
    ),
)
