package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KlagebehandlingAvbrutt
import java.time.LocalDateTime

data class KlagebehandlingAvbruttDTO(
    val avbruttAv: String,
    val avbruttTidspunkt: LocalDateTime,
    val status: AvbruttKlagebehandlingStatus,
    val begrunnelse: String?,
)

fun KlagebehandlingAvbrutt.toDTO() = KlagebehandlingAvbruttDTO(
    avbruttAv = saksbehandler,
    avbruttTidspunkt = tidspunkt,
    status = status,
    begrunnelse = begrunnelse?.value,
)
