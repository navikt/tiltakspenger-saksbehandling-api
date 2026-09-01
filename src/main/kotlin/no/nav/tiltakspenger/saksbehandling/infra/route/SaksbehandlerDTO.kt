package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

data class SaksbehandlerDTO(
    val navIdent: String,
    val brukernavn: String,
    val epost: String,
    val roller: List<SaksbehandlerRolleDTO>,
) {

    enum class SaksbehandlerRolleDTO {
        SAKSBEHANDLER,
        BESLUTTER,
        VEILEDER,
        UTVIKLER,
        TILBAKEKREVING,
    }
}

fun Saksbehandler.toSaksbehandlerDTO(): SaksbehandlerDTO =
    SaksbehandlerDTO(
        navIdent = navIdent,
        brukernavn = brukernavn,
        epost = epost,
        roller = roller.map { it.toRolleDTO() },
    )

private fun Saksbehandlerrolle.toRolleDTO(): SaksbehandlerDTO.SaksbehandlerRolleDTO =
    when (this) {
        Saksbehandlerrolle.SAKSBEHANDLER -> SaksbehandlerDTO.SaksbehandlerRolleDTO.SAKSBEHANDLER
        Saksbehandlerrolle.BESLUTTER -> SaksbehandlerDTO.SaksbehandlerRolleDTO.BESLUTTER
        Saksbehandlerrolle.VEILEDER -> SaksbehandlerDTO.SaksbehandlerRolleDTO.VEILEDER
        Saksbehandlerrolle.UTVIKLER -> SaksbehandlerDTO.SaksbehandlerRolleDTO.UTVIKLER
        Saksbehandlerrolle.TILBAKEKREVING -> SaksbehandlerDTO.SaksbehandlerRolleDTO.TILBAKEKREVING
    }
