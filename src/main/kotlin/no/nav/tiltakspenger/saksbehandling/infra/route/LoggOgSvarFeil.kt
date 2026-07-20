package no.nav.tiltakspenger.saksbehandling.infra.route

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.ktor.common.respondJson
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.felles.Loggbar
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration

/**
 * Ferdig setning som legges på slutten av vanlige logglinjer som har en tilhørende sikkerlogg-linje.
 * Lenken går til appens logger i Google Cloud Console for riktig miljø, der sikkerloggen (team-logs) kan leses.
 * Samme mønster som `SE_SIKKERLOGG` i tiltakspenger-arena.
 */
// TODO jah: Ved flytt til libs kan container_name og prosjekt utledes fra NAIS_APP_NAME og GCP_TEAM_PROJECT_ID i stedet for å hardkodes per app.
val SE_SIKKERLOGG: String by lazy {
    val prosjekt = if (Configuration.isProd()) "tpts-prod-b5ff" else "tpts-dev-6211"
    "Se sikkerlogg for mer kontekst: " +
        "https://console.cloud.google.com/logs/query;query=resource.labels.container_name%3D%22tiltakspenger-saksbehandling-api%22?project=$prosjekt"
}

/**
 * Logger nøyaktig én warn-linje for en forventet domenefeil og svarer med tilhørende status og feilkropp.
 *
 * Linjen kombinerer feilens egen [Loggbar.loggkontekst] med det route-laget vet ([operasjon] og [kontekst]).
 * Dersom feilen har en [Loggbar.sikkerloggkontekst] logges i tillegg én parallell warn-linje i sikkerlogg, og vanlig logg henviser dit med lenke ([SE_SIKKERLOGG]).
 * Suksess-stien skal ikke logge noe i routen - CallLogging dekker selve requesten.
 */
// TODO jah: Flytt til tiltakspenger-libs (ktor-common) sammen med Loggbar når mønsteret har satt seg i flere routes.
suspend fun ApplicationCall.loggOgSvarFeil(
    logger: KLogger,
    operasjon: String,
    feil: Loggbar,
    statusOgErrorJson: Pair<HttpStatusCode, ErrorJson>,
    kontekst: String,
) {
    val (status, errorJson) = statusOgErrorJson
    val sikkerloggkontekst = feil.sikkerloggkontekst
    val henvisning = if (sikkerloggkontekst != null) " $SE_SIKKERLOGG" else ""
    logger.warn(feil.loggkontekst.underliggendeFeil) {
        "$operasjon feilet: ${feil.loggkontekst.melding}. Svarer ${status.value} '${errorJson.kode}'. $kontekst.$henvisning"
    }
    if (sikkerloggkontekst != null) {
        Sikkerlogg.warn(sikkerloggkontekst.underliggendeFeil) {
            "$operasjon feilet: ${sikkerloggkontekst.melding}. $kontekst"
        }
    }
    respondJson(statusAndValue = status to errorJson)
}
