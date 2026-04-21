package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

fun KanIkkeIverksetteUtbetaling.tilErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> HttpStatusCode.InternalServerError to ErrorJson(
            "Simulering mangler - Behandlinger med utbetaling må simuleres for å kunne gå videre.",
            "må_simuleres",
        )

        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandling med justering på tvers av måneder eller meldeperioder støttes ikke på nåværende tidspunkt.",
            "støtter_ikke_justering",
        )

        KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer -> HttpStatusCode.Conflict to ErrorJson(
            "Kontrollsimuleringen har endringer sammenlignet med forrige simulering.",
            "simulering_endret",
        )

        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling -> HttpStatusCode.BadRequest to ErrorJson(
            "Vedtak som fører til feilutbetaling støttes kun ved omgjøring.",
            "feil_behandlingstype_for_feilutbetaling",
        )

        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering -> HttpStatusCode.BadRequest to ErrorJson(
            "Vedtak som fører til justeringer i utbetalingen støttes kun ved omgjøring.",
            "feil_behandlingstype_for_justering",
        )
    }
}
