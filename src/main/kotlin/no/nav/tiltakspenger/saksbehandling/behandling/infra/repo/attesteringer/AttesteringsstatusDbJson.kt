package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer

import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus

internal enum class AttesteringsstatusDbJson {
    GODKJENT,
    SENDT_TILBAKE,
}

fun String.toAttesteringsstatus(): no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus =
    when (AttesteringsstatusDbJson.valueOf(this)) {
        AttesteringsstatusDbJson.GODKJENT -> no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus.GODKJENT
        AttesteringsstatusDbJson.SENDT_TILBAKE -> no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus.SENDT_TILBAKE
    }

fun no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus.toDbJson(): String =
    when (this) {
        no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus.GODKJENT -> AttesteringsstatusDbJson.GODKJENT
        no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus.SENDT_TILBAKE -> AttesteringsstatusDbJson.SENDT_TILBAKE
    }.toString()
