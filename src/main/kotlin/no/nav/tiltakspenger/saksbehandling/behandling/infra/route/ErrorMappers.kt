package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeForlenge
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeOppdatereBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeOppdatereOmgjøring
import no.nav.tiltakspenger.saksbehandling.infra.route.Standardfeil

/**
 * Feilresponsen til saksbehandler når et annet vedtak har omgjort de samme periodene etter at omgjøringen sist ble oppdatert.
 * Saksbehandler eier behandlingen på dette steget og kan rette vedtaksperioden selv.
 */
val omgjøringsgrunnlagetErEndretForSaksbehandler: Pair<HttpStatusCode, ErrorJson> = HttpStatusCode.BadRequest to ErrorJson(
    "Et annet vedtak har endret grunnlaget for omgjøringen. Vedtaksperioden må oppdateres før behandlingen kan sendes til beslutning.",
    "omgjøringsgrunnlaget_er_endret",
)

/**
 * Samme feil sett fra beslutter ved iverksetting.
 * Beslutter kan ikke endre vedtaksperioden selv og må sende behandlingen tilbake.
 */
val omgjøringsgrunnlagetErEndretForBeslutter: Pair<HttpStatusCode, ErrorJson> = HttpStatusCode.BadRequest to ErrorJson(
    "Et annet vedtak har endret grunnlaget for omgjøringen. Behandlingen må sendes tilbake til saksbehandler for oppdatering av vedtaksperioden.",
    "omgjøringsgrunnlaget_er_endret",
)

fun KanIkkeOppdatereBehandling.tilStatusOgErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
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

    is KanIkkeOppdatereBehandling.UgyldigPeriodeForStans -> HttpStatusCode.BadRequest to ErrorJson(
        "Ugyldig periode for stans: ${this.årsak}",
        "ugyldig_periode_for_stans",
    )

    KanIkkeOppdatereOmgjøring.MåOmgjøreMinstEttVedtak -> HttpStatusCode.BadRequest to ErrorJson(
        "Må omgjøre minst ett vedtak",
        "må_omgjøre_minst_ett_vedtak",
    )

    KanIkkeOppdatereOmgjøring.VedtaksperiodeMåInneholdeInnvilgelsesperiodene -> HttpStatusCode.BadRequest to ErrorJson(
        "Vedtaksperioden må inneholde alle innvilgelsesperiodene",
        "vedtaksperiode_må_inneholde_innvilgelsesperiodene",
    )

    is KanIkkeOppdatereOmgjøring.UgyldigPeriodeForOpphør -> HttpStatusCode.BadRequest to ErrorJson(
        "Ugyldig periode for opphør: ${this.årsak}",
        "ugyldig_periode_for_opphør",
    )

    is KanIkkeOppdatereOmgjøring.MåHaFritekstForValgteHjemler -> HttpStatusCode.BadRequest to ErrorJson(
        "For valgt hjemmel må vedtaket begrunnes med fritekst: ${this.hjemler}",
        "valgte_hjemler_krever_fritekst",
    )

    KanIkkeForlenge.VedtaketKanIkkeLengerForlenges -> HttpStatusCode.Conflict to ErrorJson(
        "Vedtaket som forlenges, kan ikke lenger forlenges fra samme dato. Forlengelsen må avbrytes og startes på nytt.",
        "vedtaket_kan_ikke_lenger_forlenges",
    )

    is KanIkkeForlenge.TilOgMedMåVæreEtterVedtaket -> HttpStatusCode.BadRequest to ErrorJson(
        "Til og med-datoen må være etter ${this.sisteInnvilgedeDag}, som er siste innvilgede dag i vedtaket som forlenges",
        "forlengelse_til_og_med_må_være_etter_vedtaket",
    )

    is KanIkkeForlenge.OverlapperAndreVedtak -> HttpStatusCode.BadRequest to ErrorJson(
        "Forlengelsen ${this.forlengelsesperiode} overlapper andre vedtak",
        "forlengelse_overlapper_andre_vedtak",
    )

    is KanIkkeForlenge.TiltaksdeltakelsenDekkerIkkeForlengelsen -> HttpStatusCode.BadRequest to ErrorJson(
        "Tiltaksdeltakelsen dekker ikke hele forlengelsen ${this.forlengelsesperiode}",
        "tiltaksdeltakelsen_dekker_ikke_forlengelsen",
    )

    is KanIkkeForlenge.BarnetilleggetGårUtoverForlengelsen -> HttpStatusCode.BadRequest to ErrorJson(
        "Barnetillegget må være innenfor forlengelsen ${this.forlengelsesperiode}",
        "barnetillegget_går_utover_forlengelsen",
    )

    KanIkkeOppdatereBehandling.UgyldigeMeldeperioderHelg -> HttpStatusCode.Conflict to ErrorJson(
        "Behandlingen har meldeperioder med kun rett i helg, men saken er ikke markert for melding på helgedager",
        "ugyldige_meldeperioder_for_helg",
    )

    KanIkkeOppdatereBehandling.InnvilgelsesperiodeHarIngenOverlappMedTiltaksdeltakelserFraSaksopplysninger -> HttpStatusCode.BadRequest to ErrorJson(
        "Innvilgelsesperiode har ingen overlapp med tiltaksdeltakelser fra saksopplysninger",
        "innvilgelsesperiode_har_ingen_overlapp_med_tiltaksdeltakelser_fra_saksopplysninger",
    )
}
