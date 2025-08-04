package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

internal fun KanIkkeOppdatereBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
        this.eiesAvSaksbehandler,
    )

    KanIkkeOppdatereBehandling.MåVæreUnderBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke i status 'UNDER_BEHANDLING' for å utføre handlingen",
        "behandlingen_er_ikke_i_status_under_behandling",
    )

    KanIkkeOppdatereBehandling.InnvilgelsesperiodenOverlapperMedUtbetaltPeriode,
    KanIkkeOppdatereBehandling.StøtterIkkeTilbakekreving,
    -> HttpStatusCode.BadRequest to ErrorJson(
        "Innvilgelsesperioden overlapper med en eller flere utbetalingsperioder",
        "innvilgelsesperioden_overlapper_med_utbetalingsperiode",
    )
}
