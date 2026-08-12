package no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkAntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkVentestatus
import no.nav.tiltakspenger.saksbehandling.benk.service.BenkRespons
import no.nav.tiltakspenger.saksbehandling.benk.service.TilgangsfiltrertBenkOversikt

/**
 * Wiretypene for benk v2.
 *
 * Feltnavnene er kontrakten mot frontendens `lib/benk/v2/typer`, og fellesfeltene ligger flatt på hver rad — ikke under et `felles`-objekt — fordi det er slik frontenden leser dem.
 */

data class BenkResponsDTO(
    val tab: BenkFaneDTO,
    val antallPerTab: Map<BenkFaneDTO, Int>,
    val oversikt: BenkOversiktDTO,
    /**
     * Satt når requesten ikke lot seg tolke og benken derfor svarer med en standardvisning.
     * Frontenden viser meldingen, slik at saksbehandler ser at filtrene ikke slo til.
     */
    val error: String? = null,
)

data class BenkOversiktDTO(
    val behandlinger: List<BenkBehandlingDTO>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
    val limit: Int,
    /** Identene tildelt en rad i fanen, ufiltrert — valgene i benkens nedtrekksliste for saksbehandler/beslutter. */
    val saksbehandlere: List<String>,
    val besluttere: List<String>,
)

enum class BenkFaneDTO {
    SØKNADER,
    REVURDERINGER,
    MELDEKORT,
    KLAGE,
    TILBAKEKREVING,
}

enum class BenkBehandlingsstatusDTO {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    KLAR_TIL_FERDIGSTILLING,
}

enum class BenkBehandlingstypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
    KLAGEBEHANDLING,
    TILBAKEKREVING,
}

data class BenkVentestatusDTO(
    val erSattPåVent: Boolean,
    val begrunnelse: String?,
    val frist: String?,
)

sealed interface BenkBehandlingDTO {
    val type: BenkBehandlingstypeDTO
    val id: String
    val sakId: String
    val fnr: String
    val saksnummer: String
    val startet: String
    val sistEndret: String
    val saksbehandler: String?
    val beslutter: String?
    val erUnderkjent: Boolean
    val ventestatus: BenkVentestatusDTO
}

fun BenkFane.toDTO(): BenkFaneDTO = when (this) {
    BenkFane.SØKNADER -> BenkFaneDTO.SØKNADER
    BenkFane.REVURDERINGER -> BenkFaneDTO.REVURDERINGER
    BenkFane.MELDEKORT -> BenkFaneDTO.MELDEKORT
    BenkFane.KLAGE -> BenkFaneDTO.KLAGE
    BenkFane.TILBAKEKREVING -> BenkFaneDTO.TILBAKEKREVING
}

fun BenkAntallPerFane.toDTO(): Map<BenkFaneDTO, Int> = mapOf(
    BenkFaneDTO.SØKNADER to søknader,
    BenkFaneDTO.REVURDERINGER to revurderinger,
    BenkFaneDTO.MELDEKORT to meldekort,
    BenkFaneDTO.KLAGE to klage,
    BenkFaneDTO.TILBAKEKREVING to tilbakekreving,
)

fun <T : BenkBehandling> BenkRespons<T>.toDTO(
    fane: BenkFane,
    saksbehandler: Saksbehandler,
    error: String? = null,
): BenkResponsDTO =
    BenkResponsDTO(
        tab = fane.toDTO(),
        antallPerTab = antallPerFane.toDTO(),
        oversikt = oversikt.toDTO(saksbehandler),
        error = error,
    )

private fun <T : BenkBehandling> TilgangsfiltrertBenkOversikt<T>.toDTO(saksbehandler: Saksbehandler): BenkOversiktDTO = BenkOversiktDTO(
    behandlinger = behandlinger.map { it.toDTO(saksbehandler) },
    totalAntall = totalAntall,
    totalAntallUfiltrert = totalAntallUfiltrert,
    antallFiltrertPgaTilgang = antallFiltrertPgaTilgang,
    limit = limit,
    saksbehandlere = saksbehandlere,
    besluttere = besluttere,
)

private fun BenkBehandling.toDTO(saksbehandler: Saksbehandler): BenkBehandlingDTO = when (this) {
    is BenkSøknadsbehandling -> toDTO(saksbehandler)
    is BenkRevurdering -> toDTO(saksbehandler)
    is BenkMeldekort -> toDTO(saksbehandler)
    is BenkKlagebehandling -> toDTO()
    is BenkTilbakekreving -> toDTO(saksbehandler)
}

fun BenkVentestatus.toDTO(): BenkVentestatusDTO = BenkVentestatusDTO(
    erSattPåVent = erSattPåVent,
    begrunnelse = begrunnelse,
    frist = frist?.toString(),
)

fun BenkBehandlingsstatus.toDTO(): BenkBehandlingsstatusDTO = when (this) {
    BenkBehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> BenkBehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING
    BenkBehandlingsstatus.KLAR_TIL_BEHANDLING -> BenkBehandlingsstatusDTO.KLAR_TIL_BEHANDLING
    BenkBehandlingsstatus.UNDER_BEHANDLING -> BenkBehandlingsstatusDTO.UNDER_BEHANDLING
    BenkBehandlingsstatus.KLAR_TIL_BESLUTNING -> BenkBehandlingsstatusDTO.KLAR_TIL_BESLUTNING
    BenkBehandlingsstatus.UNDER_BESLUTNING -> BenkBehandlingsstatusDTO.UNDER_BESLUTNING
    BenkBehandlingsstatus.KLAR_TIL_FERDIGSTILLING -> BenkBehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING
}

fun BenkBehandlingsstatusDTO.tilDomene(): BenkBehandlingsstatus = when (this) {
    BenkBehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING -> BenkBehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
    BenkBehandlingsstatusDTO.KLAR_TIL_BEHANDLING -> BenkBehandlingsstatus.KLAR_TIL_BEHANDLING
    BenkBehandlingsstatusDTO.UNDER_BEHANDLING -> BenkBehandlingsstatus.UNDER_BEHANDLING
    BenkBehandlingsstatusDTO.KLAR_TIL_BESLUTNING -> BenkBehandlingsstatus.KLAR_TIL_BESLUTNING
    BenkBehandlingsstatusDTO.UNDER_BESLUTNING -> BenkBehandlingsstatus.UNDER_BESLUTNING
    BenkBehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING -> BenkBehandlingsstatus.KLAR_TIL_FERDIGSTILLING
}
