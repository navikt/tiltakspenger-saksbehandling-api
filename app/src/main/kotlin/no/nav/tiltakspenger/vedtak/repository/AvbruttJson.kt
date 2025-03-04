package no.nav.tiltakspenger.vedtak.repository

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Avbrutt
import java.time.LocalDateTime

data class AvbruttJson(
    val begrunnelse: String,
    val avbruttAv: String,
    val avbruttTidspunkt: String,
) {
    fun toAvbrutt(): Avbrutt {
        return Avbrutt(
            tidspunkt = LocalDateTime.parse(avbruttTidspunkt),
            saksbehandler = avbruttAv,
            begrunnelse = begrunnelse,
        )
    }
}

fun Avbrutt.toDbJson(): AvbruttJson = AvbruttJson(
    begrunnelse = this.begrunnelse,
    avbruttAv = this.saksbehandler,
    avbruttTidspunkt = this.tidspunkt.toString(),
)

fun String.toAvbrutt(): Avbrutt {
    val json = deserialize<AvbruttJson>(this)
    return json.toAvbrutt()
}
