package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Valideringsfeil
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

internal fun Valideringsfeil.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    Valideringsfeil.BehandlingenErIkkeUnderBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke i status 'UNDER_BEHANDLING' for å utføre handlingen",
        "behandlingen_er_ikke_i_status_under_behandling",
    )
    is Valideringsfeil.UtdøvendeSaksbehandlerErIkkePåBehandlingen -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(this.eiesAv)
}
