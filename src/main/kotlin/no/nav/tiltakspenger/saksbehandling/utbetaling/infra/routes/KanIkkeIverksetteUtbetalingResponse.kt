package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.routes

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling

fun KanIkkeIverksetteUtbetaling.tilUtbetalingErrorJson(): Pair<HttpStatusCode, ErrorJson> {
    return when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> HttpStatusCode.InternalServerError to ErrorJson(
            "Behandling med utbetaling må simuleres for å kunne sende til beslutning",
            "må_simuleres",
        )

        KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandling med feilutbetaling støttes ikke på nåværende tidspunkt",
            "støtter_ikke_feilutbetaling",
        )

        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> HttpStatusCode.BadRequest to ErrorJson(
            "Behandling med justering på tvers av måneder eller meldeperioder støttes ikke på nåværende tidspunkt",
            "støtter_ikke_justering",
        )

        KanIkkeIverksetteUtbetaling.SimuleringHarEndringer -> HttpStatusCode.Conflict to ErrorJson(
            "Simulering av utbetaling har endret seg siden behandlingen ble sendt til beslutning.",
            "simulering_endret",
        )
    }
}
