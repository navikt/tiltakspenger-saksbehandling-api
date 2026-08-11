package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO

enum class BenkRevurderingResultatDTO {
    STANS,
    REVURDERING_INNVILGELSE,
    OMGJØRING,
    OMGJØRING_OPPHØR,
    OMGJØRING_IKKE_VALGT,
}

data class BenkRevurderingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.REVURDERING,
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
    val resultat: BenkRevurderingResultatDTO?,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

fun BenkRevurdering.toDTO(saksbehandler: Saksbehandler): BenkRevurderingDTO = BenkRevurderingDTO(
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
    resultat = resultat?.toDTO(),
    gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
)

private fun BenkRevurderingResultat.toDTO(): BenkRevurderingResultatDTO = when (this) {
    BenkRevurderingResultat.STANS -> BenkRevurderingResultatDTO.STANS
    BenkRevurderingResultat.REVURDERING_INNVILGELSE -> BenkRevurderingResultatDTO.REVURDERING_INNVILGELSE
    BenkRevurderingResultat.OMGJØRING -> BenkRevurderingResultatDTO.OMGJØRING
    BenkRevurderingResultat.OMGJØRING_OPPHØR -> BenkRevurderingResultatDTO.OMGJØRING_OPPHØR
    BenkRevurderingResultat.OMGJØRING_IKKE_VALGT -> BenkRevurderingResultatDTO.OMGJØRING_IKKE_VALGT
}

fun BenkRevurderingResultatDTO.tilDomene(): BenkRevurderingResultat = when (this) {
    BenkRevurderingResultatDTO.STANS -> BenkRevurderingResultat.STANS
    BenkRevurderingResultatDTO.REVURDERING_INNVILGELSE -> BenkRevurderingResultat.REVURDERING_INNVILGELSE
    BenkRevurderingResultatDTO.OMGJØRING -> BenkRevurderingResultat.OMGJØRING
    BenkRevurderingResultatDTO.OMGJØRING_OPPHØR -> BenkRevurderingResultat.OMGJØRING_OPPHØR
    BenkRevurderingResultatDTO.OMGJØRING_IKKE_VALGT -> BenkRevurderingResultat.OMGJØRING_IKKE_VALGT
}
