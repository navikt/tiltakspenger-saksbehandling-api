package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

fun KunneIkkeSimulere.tilSimuleringErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        is KunneIkkeSimulere.Stengt -> HttpStatusCode.ServiceUnavailable to ErrorJson(
            "Økonomisystemet er stengt. Typisk åpningstider er mellom 6 og 21 på hverdager og visse lørdager.",
            "økonomisystemet_er_stengt",
        )

        is KunneIkkeSimulere.Timeout -> HttpStatusCode.RequestTimeout to ErrorJson(
            "Tjenesten for simulering svarte ikke (time-out). Du kan prøve igjen.",
            "timeout_ved_simulering",
        )

        is KunneIkkeSimulere.UkjentFeil -> HttpStatusCode.InternalServerError to ErrorJson(
            "Ukjent feil ved simulering",
            "ukjent_feil_ved_simulering",
        )
    }
}
