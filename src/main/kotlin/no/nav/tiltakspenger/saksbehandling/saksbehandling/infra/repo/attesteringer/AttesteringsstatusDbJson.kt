package no.nav.tiltakspenger.saksbehandling.saksbehandling.infra.repo.attesteringer

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attesteringsstatus

internal enum class AttesteringsstatusDbJson {
    GODKJENT,
    SENDT_TILBAKE,
}

fun String.toAttesteringsstatus(): Attesteringsstatus =
    when (AttesteringsstatusDbJson.valueOf(this)) {
        AttesteringsstatusDbJson.GODKJENT -> Attesteringsstatus.GODKJENT
        AttesteringsstatusDbJson.SENDT_TILBAKE -> Attesteringsstatus.SENDT_TILBAKE
    }

fun Attesteringsstatus.toDbJson(): String =
    when (this) {
        Attesteringsstatus.GODKJENT -> AttesteringsstatusDbJson.GODKJENT
        Attesteringsstatus.SENDT_TILBAKE -> AttesteringsstatusDbJson.SENDT_TILBAKE
    }.toString()
