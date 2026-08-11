package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2AntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Fane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Ventestatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.BenkV2Respons
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.TilgangsfiltrertBenkV2Oversikt

/**
 * Wiretypene for benk v2.
 *
 * Feltnavnene er kontrakten mot frontendens `lib/benk/v2/typer`, og fellesfeltene ligger flatt på hver rad — ikke under et `felles`-objekt — fordi det er slik frontenden leser dem.
 */

data class BenkV2ResponsDTO(
    val tab: BenkV2FaneDTO,
    val antallPerTab: Map<BenkV2FaneDTO, Int>,
    val oversikt: BenkV2OversiktDTO,
    /**
     * Satt når requesten ikke lot seg tolke og benken derfor svarer med en standardvisning.
     * Frontenden viser meldingen, slik at saksbehandler ser at filtrene ikke slo til.
     */
    val error: String? = null,
)

data class BenkV2OversiktDTO(
    val behandlinger: List<BenkV2BehandlingDTO>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
    val limit: Int,
)

enum class BenkV2FaneDTO {
    SØKNADER,
    REVURDERINGER,
    MELDEKORT,
    KLAGE,
    TILBAKEKREVING,
}

enum class BenkV2BehandlingsstatusDTO {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    KLAR_TIL_FERDIGSTILLING,
}

enum class BenkV2BehandlingstypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
    KLAGEBEHANDLING,
    TILBAKEKREVING,
}

data class BenkV2VentestatusDTO(
    val erSattPåVent: Boolean,
    val begrunnelse: String?,
    val frist: String?,
)

data class BenkV2PeriodeDTO(
    val fraOgMed: String,
    val tilOgMed: String,
)

sealed interface BenkV2BehandlingDTO {
    val type: BenkV2BehandlingstypeDTO
    val id: String
    val sakId: String
    val fnr: String
    val saksnummer: String
    val startet: String
    val sistEndret: String
    val saksbehandler: String?
    val beslutter: String?
    val erUnderkjent: Boolean
    val ventestatus: BenkV2VentestatusDTO
}

fun BenkV2Fane.toDTO(): BenkV2FaneDTO = when (this) {
    BenkV2Fane.SØKNADER -> BenkV2FaneDTO.SØKNADER
    BenkV2Fane.REVURDERINGER -> BenkV2FaneDTO.REVURDERINGER
    BenkV2Fane.MELDEKORT -> BenkV2FaneDTO.MELDEKORT
    BenkV2Fane.KLAGE -> BenkV2FaneDTO.KLAGE
    BenkV2Fane.TILBAKEKREVING -> BenkV2FaneDTO.TILBAKEKREVING
}

fun BenkV2AntallPerFane.toDTO(): Map<BenkV2FaneDTO, Int> = mapOf(
    BenkV2FaneDTO.SØKNADER to søknader,
    BenkV2FaneDTO.REVURDERINGER to revurderinger,
    BenkV2FaneDTO.MELDEKORT to meldekort,
    BenkV2FaneDTO.KLAGE to klage,
    BenkV2FaneDTO.TILBAKEKREVING to tilbakekreving,
)

fun <T : BenkV2Behandling> BenkV2Respons<T>.toDTO(
    fane: BenkV2Fane,
    saksbehandler: Saksbehandler,
    error: String? = null,
): BenkV2ResponsDTO =
    BenkV2ResponsDTO(
        tab = fane.toDTO(),
        antallPerTab = antallPerFane.toDTO(),
        oversikt = oversikt.toDTO(saksbehandler),
        error = error,
    )

private fun <T : BenkV2Behandling> TilgangsfiltrertBenkV2Oversikt<T>.toDTO(saksbehandler: Saksbehandler): BenkV2OversiktDTO = BenkV2OversiktDTO(
    behandlinger = behandlinger.map { it.toDTO(saksbehandler) },
    totalAntall = totalAntall,
    totalAntallUfiltrert = totalAntallUfiltrert,
    antallFiltrertPgaTilgang = antallFiltrertPgaTilgang,
    limit = limit,
)

private fun BenkV2Behandling.toDTO(saksbehandler: Saksbehandler): BenkV2BehandlingDTO = when (this) {
    is BenkSøknadsbehandling -> toDTO(saksbehandler)
    is BenkRevurdering -> toDTO(saksbehandler)
    is BenkMeldekort -> toDTO(saksbehandler)
    is BenkKlagebehandling -> toDTO()
    is BenkTilbakekreving -> toDTO(saksbehandler)
}

fun BenkV2Ventestatus.toDTO(): BenkV2VentestatusDTO = BenkV2VentestatusDTO(
    erSattPåVent = erSattPåVent,
    begrunnelse = begrunnelse,
    frist = frist?.toString(),
)

fun BenkV2Behandlingsstatus.toDTO(): BenkV2BehandlingsstatusDTO = when (this) {
    BenkV2Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> BenkV2BehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING
    BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING -> BenkV2BehandlingsstatusDTO.KLAR_TIL_BEHANDLING
    BenkV2Behandlingsstatus.UNDER_BEHANDLING -> BenkV2BehandlingsstatusDTO.UNDER_BEHANDLING
    BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING -> BenkV2BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
    BenkV2Behandlingsstatus.UNDER_BESLUTNING -> BenkV2BehandlingsstatusDTO.UNDER_BESLUTNING
    BenkV2Behandlingsstatus.KLAR_TIL_FERDIGSTILLING -> BenkV2BehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING
}

fun BenkV2BehandlingsstatusDTO.tilDomene(): BenkV2Behandlingsstatus = when (this) {
    BenkV2BehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING -> BenkV2Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
    BenkV2BehandlingsstatusDTO.KLAR_TIL_BEHANDLING -> BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING
    BenkV2BehandlingsstatusDTO.UNDER_BEHANDLING -> BenkV2Behandlingsstatus.UNDER_BEHANDLING
    BenkV2BehandlingsstatusDTO.KLAR_TIL_BESLUTNING -> BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING
    BenkV2BehandlingsstatusDTO.UNDER_BESLUTNING -> BenkV2Behandlingsstatus.UNDER_BESLUTNING
    BenkV2BehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING -> BenkV2Behandlingsstatus.KLAR_TIL_FERDIGSTILLING
}
