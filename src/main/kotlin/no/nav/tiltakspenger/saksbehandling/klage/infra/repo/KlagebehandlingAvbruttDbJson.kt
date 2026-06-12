package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbruttKlagebehandlingStatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KlagebehandlingAvbrutt
import java.time.LocalDateTime

data class KlagebehandlingAvbruttDbJson(
    val begrunnelse: String?,
    val avbruttAv: String,
    val avbruttTidspunkt: String,
    val status: String,
)

fun KlagebehandlingAvbrutt.toDbJson(): String = KlagebehandlingAvbruttDbJson(
    begrunnelse = begrunnelse?.value,
    avbruttAv = saksbehandler,
    avbruttTidspunkt = tidspunkt.toString(),
    status = status.name,
).let { serialize(it) }

fun String.toKlagebehandlingAvbrutt(): KlagebehandlingAvbrutt {
    val json = deserialize<KlagebehandlingAvbruttDbJson>(this)
    return KlagebehandlingAvbrutt(
        tidspunkt = LocalDateTime.parse(json.avbruttTidspunkt),
        saksbehandler = json.avbruttAv,
        begrunnelse = json.begrunnelse?.toNonBlankString(),
        status = AvbruttKlagebehandlingStatus.valueOf(json.status),
    )
}
