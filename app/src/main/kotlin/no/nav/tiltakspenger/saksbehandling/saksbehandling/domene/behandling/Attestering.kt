package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.nå
import java.time.LocalDateTime

data class Attestering(
    val id: AttesteringId = AttesteringId.random(),
    val status: Attesteringsstatus,
    val begrunnelse: String?,
    val beslutter: String,
    val tidspunkt: LocalDateTime = nå(),
) {
    fun isGodkjent() = status == Attesteringsstatus.GODKJENT

    fun isUnderkjent() = status == Attesteringsstatus.SENDT_TILBAKE
}

enum class Attesteringsstatus {
    GODKJENT,
    SENDT_TILBAKE,
}
