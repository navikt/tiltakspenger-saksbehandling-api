package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingStatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO
import java.math.BigDecimal

enum class BenkTilbakekrevingStatusDTO {
    OPPRETTET,
    TIL_FORHÅNDSVARSEL,
    UNDER_FORHÅNDSVARSLING,
    TIL_BEHANDLING,
    UNDER_BEHANDLING,
    TIL_GODKJENNING,
    UNDER_GODKJENNING,
}

enum class BenkTilbakekrevingKildeDTO {
    RAMMEVEDTAK,
    MELDEKORT,
}

data class BenkTilbakekrevingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.TILBAKEKREVING,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkV2VentestatusDTO,
    val status: BenkTilbakekrevingStatusDTO,
    val beløp: BigDecimal,
    val kilde: BenkTilbakekrevingKildeDTO,
    val kravgrunnlagPeriode: BenkV2PeriodeDTO,
    val url: String,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

fun BenkTilbakekreving.toDTO(saksbehandler: Saksbehandler): BenkTilbakekrevingDTO = BenkTilbakekrevingDTO(
    id = id.toString(),
    sakId = felles.sakId.toString(),
    fnr = felles.fnr.verdi,
    saksnummer = felles.saksnummer.verdi,
    startet = felles.startet.toString(),
    sistEndret = felles.sistEndret.toString(),
    saksbehandler = felles.saksbehandler,
    beslutter = felles.beslutter,
    erUnderkjent = felles.erUnderkjent,
    ventestatus = felles.ventestatus.toDTO(),
    status = status.toDTO(),
    beløp = beløp,
    kilde = kilde.toDTO(),
    kravgrunnlagPeriode = BenkV2PeriodeDTO(
        kravgrunnlagPeriode.fraOgMed.toString(),
        kravgrunnlagPeriode.tilOgMed.toString(),
    ),
    url = url,
    gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
)

private fun BenkTilbakekrevingStatus.toDTO(): BenkTilbakekrevingStatusDTO = when (this) {
    BenkTilbakekrevingStatus.OPPRETTET -> BenkTilbakekrevingStatusDTO.OPPRETTET
    BenkTilbakekrevingStatus.TIL_FORHÅNDSVARSEL -> BenkTilbakekrevingStatusDTO.TIL_FORHÅNDSVARSEL
    BenkTilbakekrevingStatus.UNDER_FORHÅNDSVARSLING -> BenkTilbakekrevingStatusDTO.UNDER_FORHÅNDSVARSLING
    BenkTilbakekrevingStatus.TIL_BEHANDLING -> BenkTilbakekrevingStatusDTO.TIL_BEHANDLING
    BenkTilbakekrevingStatus.UNDER_BEHANDLING -> BenkTilbakekrevingStatusDTO.UNDER_BEHANDLING
    BenkTilbakekrevingStatus.TIL_GODKJENNING -> BenkTilbakekrevingStatusDTO.TIL_GODKJENNING
    BenkTilbakekrevingStatus.UNDER_GODKJENNING -> BenkTilbakekrevingStatusDTO.UNDER_GODKJENNING
}

private fun BenkTilbakekrevingKilde.toDTO(): BenkTilbakekrevingKildeDTO = when (this) {
    BenkTilbakekrevingKilde.RAMMEVEDTAK -> BenkTilbakekrevingKildeDTO.RAMMEVEDTAK
    BenkTilbakekrevingKilde.MELDEKORT -> BenkTilbakekrevingKildeDTO.MELDEKORT
}

fun BenkTilbakekrevingStatusDTO.tilDomene(): BenkTilbakekrevingStatus = when (this) {
    BenkTilbakekrevingStatusDTO.OPPRETTET -> BenkTilbakekrevingStatus.OPPRETTET
    BenkTilbakekrevingStatusDTO.TIL_FORHÅNDSVARSEL -> BenkTilbakekrevingStatus.TIL_FORHÅNDSVARSEL
    BenkTilbakekrevingStatusDTO.UNDER_FORHÅNDSVARSLING -> BenkTilbakekrevingStatus.UNDER_FORHÅNDSVARSLING
    BenkTilbakekrevingStatusDTO.TIL_BEHANDLING -> BenkTilbakekrevingStatus.TIL_BEHANDLING
    BenkTilbakekrevingStatusDTO.UNDER_BEHANDLING -> BenkTilbakekrevingStatus.UNDER_BEHANDLING
    BenkTilbakekrevingStatusDTO.TIL_GODKJENNING -> BenkTilbakekrevingStatus.TIL_GODKJENNING
    BenkTilbakekrevingStatusDTO.UNDER_GODKJENNING -> BenkTilbakekrevingStatus.UNDER_GODKJENNING
}

fun BenkTilbakekrevingKildeDTO.tilDomene(): BenkTilbakekrevingKilde = when (this) {
    BenkTilbakekrevingKildeDTO.RAMMEVEDTAK -> BenkTilbakekrevingKilde.RAMMEVEDTAK
    BenkTilbakekrevingKildeDTO.MELDEKORT -> BenkTilbakekrevingKilde.MELDEKORT
}
