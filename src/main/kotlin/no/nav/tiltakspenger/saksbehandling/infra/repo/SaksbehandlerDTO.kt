package no.nav.tiltakspenger.saksbehandling.infra.repo

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

data class SaksbehandlerDTO(
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
        HENT_ELLER_OPPRETT_SAK,
        LAGRE_SOKNAD,
        LAGRE_MELDEKORT,

        // Systemadministrator (oss)
        DRIFT,
        BESLUTTER,
    }
}

fun Saksbehandler.toSaksbehandlerDTO(): SaksbehandlerDTO =
    SaksbehandlerDTO(
        navIdent = navIdent,
        brukernavn = brukernavn,
        epost = epost,
        roller = roller.map { it.toRolleDTO() },
    )

private fun Saksbehandlerrolle.toRolleDTO(): SaksbehandlerDTO.RolleDTO =
    when (this) {
        Saksbehandlerrolle.SAKSBEHANDLER -> SaksbehandlerDTO.RolleDTO.SAKSBEHANDLER

        Saksbehandlerrolle.BESLUTTER -> SaksbehandlerDTO.RolleDTO.BESLUTTER

        Saksbehandlerrolle.DRIFT -> SaksbehandlerDTO.RolleDTO.DRIFT

        // Disse rollene har vi ikke noe forhold til lengre, tilgangskontroll skjer via tilgangsmaskinen
        Saksbehandlerrolle.FORTROLIG_ADRESSE -> SaksbehandlerDTO.RolleDTO.FORTROLIG_ADRESSE

        Saksbehandlerrolle.STRENGT_FORTROLIG_ADRESSE -> SaksbehandlerDTO.RolleDTO.STRENGT_FORTROLIG_ADRESSE

        Saksbehandlerrolle.SKJERMING -> SaksbehandlerDTO.RolleDTO.SKJERMING
    }
