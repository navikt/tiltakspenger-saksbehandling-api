package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import java.time.LocalDateTime

data class AttesteringDTO(
    val endretAv: String,
    val status: Attesteringsstatus,
    val begrunnelse: String?,
    val endretTidspunkt: LocalDateTime,
)

internal fun List<Attestering>.toAttesteringDTO(): List<AttesteringDTO> {
    return this.map { it.toAttesteringDTO() }
}

internal fun Attestering.toAttesteringDTO() = AttesteringDTO(
    endretAv = this.beslutter,
    status = this.status,
    begrunnelse = this.begrunnelse?.value,
    endretTidspunkt = this.tidspunkt,
)
