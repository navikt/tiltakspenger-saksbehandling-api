package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeOppdatereOmgjøring
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

internal fun KanIkkeOppdatereBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KanIkkeOppdatereBehandling.BehandlingenEiesAvAnnenSaksbehandler -> HttpStatusCode.BadRequest to Standardfeil.behandlingenEiesAvAnnenSaksbehandler(
        this.eiesAvSaksbehandler,
    )

    KanIkkeOppdatereBehandling.MåVæreUnderBehandling -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er ikke i status 'UNDER_BEHANDLING' for å utføre handlingen",
        "behandlingen_er_ikke_i_status_under_behandling",
    )

    KanIkkeOppdatereBehandling.InnvilgelsesperiodenOverlapperMedUtbetaltPeriode -> HttpStatusCode.BadRequest to ErrorJson(
        "Innvilgelsesperioden overlapper med en eller flere utbetalingsperioder",
        "innvilgelsesperioden_overlapper_med_utbetalingsperiode",
    )

    KanIkkeOppdatereBehandling.ErPaVent -> HttpStatusCode.BadRequest to ErrorJson(
        "Behandlingen er satt på vent",
        "behandlingen_er_pa_vent",
    )

    KanIkkeOppdatereBehandling.KanIkkeOpphøre -> HttpStatusCode.BadRequest to ErrorJson(
        "Denne behandlingstypen støtter ikke opphør",
        "kan_ikke_opphøre",
    )

    KanIkkeOppdatereOmgjøring.KanIkkeOmgjøreFlereVedtak -> HttpStatusCode.BadRequest to ErrorJson(
        "En omgjøring kan kun omgjøre ett tidligere vedtak",
        "kan_ikke_omgjøre_flere_vedtak",
    )

    KanIkkeOppdatereOmgjøring.MåOmgjøreEnSammenhengendePeriode -> HttpStatusCode.BadRequest to ErrorJson(
        "Kan foreløpig ikke omgjøre vedtak som ikke har en sammenhengende gjeldende periode",
        "perioder_som_omgjøres_må_være_sammenhengende",
    )

    KanIkkeOppdatereOmgjøring.MåOmgjøreAngittVedtak -> HttpStatusCode.BadRequest to ErrorJson(
        "Må omgjøre angitt vedtak",
        "må_omgjøre_angitt_vedtak",
    )

    KanIkkeOppdatereOmgjøring.MåOmgjøreMinstEttVedtak -> HttpStatusCode.BadRequest to ErrorJson(
        "Må omgjøre minst ett vedtak",
        "må_omgjøre_minst_ett_vedtak",
    )

    KanIkkeOppdatereOmgjøring.VedtaksperiodeMåInneholdeInnvilgelsesperiodene -> HttpStatusCode.BadRequest to ErrorJson(
        "Vedtaksperioden må inneholde alle innvilgelsesperiodene",
        "vedtaksperiode_må_inneholde_innvilgelsesperiodene",
    )
}
