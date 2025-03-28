package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler

internal data class SaksbehandlerDbJson(
    val navIdent: String,
)

internal fun Saksbehandler.toDbJson(): SaksbehandlerDbJson = SaksbehandlerDbJson(navIdent = navIdent)
