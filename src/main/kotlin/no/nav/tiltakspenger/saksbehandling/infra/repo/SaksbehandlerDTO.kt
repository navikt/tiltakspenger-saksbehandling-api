package no.nav.tiltakspenger.saksbehandling.infra.repo

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

internal data class SaksbehandlerDTO(
    val navIdent: String,
    val brukernavn: String,
    val epost: String,
    val roller: List<RolleDTO>,
) {
    @Suppress("unused")
    enum class RolleDTO {
        SAKSBEHANDLER,
        FORTROLIG_ADRESSE,
        STRENGT_FORTROLIG_ADRESSE,
        SKJERMING,
        LAGE_HENDELSER,
        HENTE_DATA,

        // Systemadministrator (oss)
        DRIFT,
        BESLUTTER,
    }
}

internal fun Saksbehandler.toSaksbehandlerDTO(): SaksbehandlerDTO =
    SaksbehandlerDTO(
        navIdent = navIdent,
        brukernavn = brukernavn,
        epost = epost,
        roller = roller.map { it.toRolleDTO() },
    )

internal fun Saksbehandlerrolle.toRolleDTO(): SaksbehandlerDTO.RolleDTO =
    when (this) {
        Saksbehandlerrolle.SAKSBEHANDLER -> SaksbehandlerDTO.RolleDTO.SAKSBEHANDLER
        Saksbehandlerrolle.FORTROLIG_ADRESSE -> SaksbehandlerDTO.RolleDTO.FORTROLIG_ADRESSE
        Saksbehandlerrolle.STRENGT_FORTROLIG_ADRESSE -> SaksbehandlerDTO.RolleDTO.STRENGT_FORTROLIG_ADRESSE
        Saksbehandlerrolle.SKJERMING -> SaksbehandlerDTO.RolleDTO.SKJERMING
        Saksbehandlerrolle.DRIFT -> SaksbehandlerDTO.RolleDTO.DRIFT
        Saksbehandlerrolle.BESLUTTER -> SaksbehandlerDTO.RolleDTO.BESLUTTER
    }
