package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import java.time.LocalDateTime

private data class ForsøkshistorikkDbJson(
    val forrigeForsøk: String,
    val antallForsøk: Long,
)

fun Forsøkshistorikk.toDbJson(): String {
    return serialize(
        ForsøkshistorikkDbJson(
            forrigeForsøk = forrigeForsøk.toString(),
            antallForsøk = antallForsøk,
        ),
    )
}

fun String.toForsøkshistorikk(): Forsøkshistorikk {
    val db = deserialize<ForsøkshistorikkDbJson>(this)
    return Forsøkshistorikk(
        forrigeForsøk = LocalDateTime.parse(db.forrigeForsøk),
        antallForsøk = db.antallForsøk,
    )
}
