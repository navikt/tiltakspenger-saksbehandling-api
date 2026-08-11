package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto

import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandlingResultat

enum class BenkKlagebehandlingResultatDTO {
    AVVIST,
    OMGJØR,
    OPPRETTHOLDT,
}

data class BenkKlagebehandlingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.KLAGEBEHANDLING,
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
    val kravtidspunkt: String,
    val resultat: BenkKlagebehandlingResultatDTO?,
) : BenkV2BehandlingDTO

fun BenkKlagebehandling.toDTO(): BenkKlagebehandlingDTO = BenkKlagebehandlingDTO(
    id = id.toString(),
    sakId = felles.sakId.toString(),
    fnr = felles.fnr.verdi,
    saksnummer = felles.saksnummer.verdi,
    startet = felles.startet.toString(),
    sistEndret = felles.sistEndret.toString(),
    saksbehandler = felles.saksbehandler,
    beslutter = null,
    erUnderkjent = felles.erUnderkjent,
    ventestatus = felles.ventestatus.toDTO(),
    status = status.toDTO(),
    kravtidspunkt = kravtidspunkt.toString(),
    resultat = resultat?.toDTO(),
)

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
