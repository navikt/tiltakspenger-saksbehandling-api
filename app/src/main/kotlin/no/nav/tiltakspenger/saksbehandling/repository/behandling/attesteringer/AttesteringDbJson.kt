package no.nav.tiltakspenger.saksbehandling.repository.behandling.attesteringer

import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId.Companion.fromString
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attestering
import java.time.LocalDateTime

internal data class AttesteringDbJson(
    val id: String,
    val status: String,
    val begrunnelse: String?,
    val beslutter: String,
    val tidspunkt: LocalDateTime,
) {
    fun toDomain(): Attestering =
        Attestering(
            id = fromString(id),
            status = status.toAttesteringsstatus(),
            begrunnelse = begrunnelse,
            beslutter = beslutter,
            tidspunkt = tidspunkt,
        )
}

internal fun Attestering.toDbJson(): AttesteringDbJson =
    AttesteringDbJson(
        id = id.toString(),
        status = status.toDbJson(),
        begrunnelse = begrunnelse,
        beslutter = beslutter,
        tidspunkt = tidspunkt,
    )
