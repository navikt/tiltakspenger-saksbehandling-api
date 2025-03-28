package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt
import java.time.LocalDateTime

data class AvbruttJson(
    val begrunnelse: String,
    val avbruttAv: String,
    val avbruttTidspunkt: String,
) {
    fun toAvbrutt(): no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt {
        return no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt(
            tidspunkt = LocalDateTime.parse(avbruttTidspunkt),
            saksbehandler = avbruttAv,
            begrunnelse = begrunnelse,
        )
    }
}

fun no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt.toDbJson(): String = AvbruttJson(
    begrunnelse = this.begrunnelse,
    avbruttAv = this.saksbehandler,
    avbruttTidspunkt = this.tidspunkt.toString(),
).let { serialize(it) }

fun String.toAvbrutt(): no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Avbrutt {
    val json = deserialize<AvbruttJson>(this)
    return json.toAvbrutt()
}
