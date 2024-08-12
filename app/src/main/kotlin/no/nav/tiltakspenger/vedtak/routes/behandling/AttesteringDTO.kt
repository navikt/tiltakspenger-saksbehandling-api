package no.nav.tiltakspenger.vedtak.routes.behandling

import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus
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
    begrunnelse = this.begrunnelse,
    endretTidspunkt = this.tidspunkt,
)
