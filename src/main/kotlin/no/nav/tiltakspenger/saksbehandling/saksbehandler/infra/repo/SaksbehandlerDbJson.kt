package no.nav.tiltakspenger.saksbehandling.saksbehandler.infra.repo

import no.nav.tiltakspenger.libs.common.Saksbehandler

data class SaksbehandlerDbJson(
    val navIdent: String,
)

fun Saksbehandler.toDbJson(): SaksbehandlerDbJson = SaksbehandlerDbJson(navIdent = navIdent)
