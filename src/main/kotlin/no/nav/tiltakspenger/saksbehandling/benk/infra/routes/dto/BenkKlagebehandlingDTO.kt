package no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto

import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet

enum class BenkKlagebehandlingStatusDTO {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_FERDIGSTILLING,
}

enum class BenkKlagebehandlingResultatDTO {
    AVVIST,
    OMGJØR,
    OPPRETTHOLDT,
}

data class BenkKlagebehandlingDTO(
    override val type: BenkBehandlingstypeDTO = BenkBehandlingstypeDTO.KLAGEBEHANDLING,
    override val id: String,
    override val sakId: SladdbarVerdi<String>,
    override val fnr: SladdbarVerdi<String>,
    override val saksnummer: SladdbarVerdi<String>,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkVentestatusDTO,
    override val tilgang: BenkTilgangDTO,
    override val personmarkører: BenkPersonmarkørerDTO,
    val status: BenkKlagebehandlingStatusDTO,
    val kravtidspunkt: String,
    val resultat: BenkKlagebehandlingResultatDTO?,
) : BenkBehandlingDTO

fun BenkKlagebehandling.toDTO(
    tilgang: BenkTilgangDTO,
    personmarkører: BenkPersonmarkørerDTO,
): BenkKlagebehandlingDTO = BenkKlagebehandlingDTO(
    id = id.toString(),
    sakId = felles.sakId.toString().ikkeSladdet(),
    fnr = felles.fnr.verdi.ikkeSladdet(),
    saksnummer = felles.saksnummer.verdi.ikkeSladdet(),
    startet = felles.startet.toString(),
    sistEndret = felles.sistEndret.toString(),
    saksbehandler = felles.saksbehandler,
    beslutter = null,
    erUnderkjent = felles.erUnderkjent,
    ventestatus = felles.ventestatus.toDTO(),
    tilgang = tilgang,
    personmarkører = personmarkører,
    status = status.toDTO(),
    kravtidspunkt = kravtidspunkt.toString(),
    resultat = resultat?.toDTO(),
)

private fun BenkKlagebehandlingStatus.toDTO(): BenkKlagebehandlingStatusDTO = when (this) {
    BenkKlagebehandlingStatus.KLAR_TIL_BEHANDLING -> BenkKlagebehandlingStatusDTO.KLAR_TIL_BEHANDLING
    BenkKlagebehandlingStatus.UNDER_BEHANDLING -> BenkKlagebehandlingStatusDTO.UNDER_BEHANDLING
    BenkKlagebehandlingStatus.KLAR_TIL_FERDIGSTILLING -> BenkKlagebehandlingStatusDTO.KLAR_TIL_FERDIGSTILLING
}

fun BenkKlagebehandlingStatusDTO.tilDomene(): BenkKlagebehandlingStatus = when (this) {
    BenkKlagebehandlingStatusDTO.KLAR_TIL_BEHANDLING -> BenkKlagebehandlingStatus.KLAR_TIL_BEHANDLING
    BenkKlagebehandlingStatusDTO.UNDER_BEHANDLING -> BenkKlagebehandlingStatus.UNDER_BEHANDLING
    BenkKlagebehandlingStatusDTO.KLAR_TIL_FERDIGSTILLING -> BenkKlagebehandlingStatus.KLAR_TIL_FERDIGSTILLING
}

private fun BenkKlagebehandlingResultat.toDTO(): BenkKlagebehandlingResultatDTO = when (this) {
    BenkKlagebehandlingResultat.AVVIST -> BenkKlagebehandlingResultatDTO.AVVIST
    BenkKlagebehandlingResultat.OMGJØR -> BenkKlagebehandlingResultatDTO.OMGJØR
    BenkKlagebehandlingResultat.OPPRETTHOLDT -> BenkKlagebehandlingResultatDTO.OPPRETTHOLDT
}

fun BenkKlagebehandlingResultatDTO.tilDomene(): BenkKlagebehandlingResultat = when (this) {
    BenkKlagebehandlingResultatDTO.AVVIST -> BenkKlagebehandlingResultat.AVVIST
    BenkKlagebehandlingResultatDTO.OMGJØR -> BenkKlagebehandlingResultat.OMGJØR
    BenkKlagebehandlingResultatDTO.OPPRETTHOLDT -> BenkKlagebehandlingResultat.OPPRETTHOLDT
}
