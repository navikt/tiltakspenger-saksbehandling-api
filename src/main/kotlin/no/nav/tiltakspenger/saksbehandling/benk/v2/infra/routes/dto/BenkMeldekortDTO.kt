package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortType
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO

/**
 * Filterverdien for meldekorttypen i meldekortfanen.
 * Til forskjell fra [BenkV2BehandlingstypeDTO], som er radens typeunion ut, er dette bare typene fanen kan filtrere på.
 */
enum class BenkMeldekortTypeDTO {
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
}

data class BenkMeldekortDTO(
    override val type: BenkV2BehandlingstypeDTO,
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
    val status: BenkV2BehandlingsstatusDTO,
    val meldeperioder: List<PeriodeDTO>,
    val beløp: Int?,
    val mottattTidspunkt: String?,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

fun BenkMeldekort.toDTO(saksbehandler: Saksbehandler): BenkMeldekortDTO = BenkMeldekortDTO(
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
    type = type.toDTO(),
    meldeperioder = meldeperioder.map { PeriodeDTO(it.fraOgMed.toString(), it.tilOgMed.toString()) },
    beløp = beløp,
    mottattTidspunkt = mottattTidspunkt?.toString(),
    gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
)

private fun BenkMeldekortType.toDTO(): BenkV2BehandlingstypeDTO = when (this) {
    BenkMeldekortType.MELDEKORTBEHANDLING -> BenkV2BehandlingstypeDTO.MELDEKORTBEHANDLING
    BenkMeldekortType.INNSENDT_MELDEKORT -> BenkV2BehandlingstypeDTO.INNSENDT_MELDEKORT
    BenkMeldekortType.KORRIGERT_MELDEKORT -> BenkV2BehandlingstypeDTO.KORRIGERT_MELDEKORT
}

fun BenkMeldekortTypeDTO.tilDomene(): BenkMeldekortType = when (this) {
    BenkMeldekortTypeDTO.MELDEKORTBEHANDLING -> BenkMeldekortType.MELDEKORTBEHANDLING
    BenkMeldekortTypeDTO.INNSENDT_MELDEKORT -> BenkMeldekortType.INNSENDT_MELDEKORT
    BenkMeldekortTypeDTO.KORRIGERT_MELDEKORT -> BenkMeldekortType.KORRIGERT_MELDEKORT
}
