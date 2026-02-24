package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson

sealed interface KunneIkkeSimulere {
    object UkjentFeil : KunneIkkeSimulere

    /** OS har åpningstider. Typisk mandag til fredag fra 6 til 21. Men det hender den er stengt på helligdager og vedlikeholdsdager også. */
    object Stengt : KunneIkkeSimulere

    object Timeout : KunneIkkeSimulere

    fun tilSimuleringErrorJson(): Pair<HttpStatusCode, ErrorJson> {
        return when (this) {
            Stengt -> HttpStatusCode.ServiceUnavailable to ErrorJson(
                "Økonomisystemet er stengt. Typisk åpningstider er mellom 6 og 21 på hverdager og visse lørdager.",
                "økonomisystemet_er_stengt",
            )

            Timeout -> HttpStatusCode.RequestTimeout to ErrorJson(
                "Tjenesten for simulering svarte ikke (time-out). Du kan prøve igjen.",
                "timeout_ved_simulering",
            )

            UkjentFeil -> HttpStatusCode.InternalServerError to ErrorJson(
                "Ukjent feil ved simulering",
                "ukjent_feil_ved_simulering",
            )
        }
    }
}
