package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.saksbehandling.felles.SattPåVentBegrunnelse
import java.time.LocalDateTime

data class SattPåVentBegrunnelseJson(
    val begrunnelse: String,
    val sattPåVentAv: String,
    val sattPåVentTidspunkt: String,
) {
    fun toSattPåVentBegrunnelse(): SattPåVentBegrunnelse {
        return SattPåVentBegrunnelse(
            tidspunkt = LocalDateTime.parse(sattPåVentTidspunkt),
            sattPåVentAv = sattPåVentAv,
            begrunnelse = begrunnelse,
        )
    }
}

fun SattPåVentBegrunnelse.toDbJson(): SattPåVentBegrunnelseJson = SattPåVentBegrunnelseJson(
    begrunnelse = this.begrunnelse,
    sattPåVentAv = this.sattPåVentAv,
    sattPåVentTidspunkt = this.tidspunkt.toString(),
)
