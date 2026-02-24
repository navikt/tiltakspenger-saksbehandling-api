package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO

data class BehandlingssammendragDTO(
    val sakId: String,
    val fnr: String,
    val saksnummer: String,
    val startet: String,
    val kravtidspunkt: String?,
    val behandlingstype: BehandlingssammendragTypeDTO,
    val status: BehandlingssammendragStatusDto?,
    val saksbehandler: String?,
    val beslutter: String?,
    val sistEndret: String?,
    val erSattPåVent: Boolean,
    val sattPåVentBegrunnelse: String?,
    val sattPåVentFrist: String?,
    val resultat: RammebehandlingResultatTypeDTO?,
)

enum class BehandlingssammendragTypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
    KLAGEBEHANDLING,
}
