package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer

import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId.Companion.fromString
import java.time.LocalDateTime

internal data class AttesteringDbJson(
    val id: String,
    val status: String,
    val begrunnelse: String?,
    val beslutter: String,
    val tidspunkt: LocalDateTime,
) {
    fun toDomain(): no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering =
        no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering(
            id = fromString(id),
            status = status.toAttesteringsstatus(),
            begrunnelse = begrunnelse?.toNonBlankString(),
            beslutter = beslutter,
            tidspunkt = tidspunkt,
        )
}

internal fun no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering.toDbJson(): AttesteringDbJson =
    AttesteringDbJson(
        id = id.toString(),
        status = status.toDbJson(),
        begrunnelse = begrunnelse?.value,
        beslutter = beslutter,
        tidspunkt = tidspunkt,
    )
