package no.nav.tiltakspenger.saksbehandling.routes.behandling.dto

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attesteringsstatus
import java.time.LocalDateTime

data class AttesteringDTO(
    val endretAv: String,
    val status: Attesteringsstatus,
    val begrunnelse: String?,
    val endretTidspunkt: LocalDateTime,
)

internal fun List<Attestering>.toDTO(): List<AttesteringDTO> {
    return this.map { it.toDTO() }
}

internal fun Attestering.toDTO() = AttesteringDTO(
    endretAv = this.beslutter,
    status = this.status,
    begrunnelse = this.begrunnelse?.value,
    endretTidspunkt = this.tidspunkt,
)
