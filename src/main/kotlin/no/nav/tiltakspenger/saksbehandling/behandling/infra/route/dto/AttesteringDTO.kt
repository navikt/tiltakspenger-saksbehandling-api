package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus
import java.time.LocalDateTime

data class AttesteringDTO(
    val endretAv: String,
    val status: no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attesteringsstatus,
    val begrunnelse: String?,
    val endretTidspunkt: LocalDateTime,
)

internal fun List<no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering>.toAttesteringDTO(): List<AttesteringDTO> {
    return this.map { it.toAttesteringDTO() }
}

internal fun no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering.toAttesteringDTO() = AttesteringDTO(
    endretAv = this.beslutter,
    status = this.status,
    begrunnelse = this.begrunnelse?.value,
    endretTidspunkt = this.tidspunkt,
)
