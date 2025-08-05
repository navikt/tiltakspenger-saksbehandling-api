package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.SattPåVent
import java.security.InvalidParameterException

data class SattPåVentJson(
    val erSattPåVent: Boolean,
    val sattPåVentBegrunnelser: List<SattPåVentBegrunnelseJson>,
)

fun String.toSattPåVent(): SattPåVent {
    val sattPåVentJson = try {
        deserialize<SattPåVentJson>(this)
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }
    return SattPåVent(
        erSattPåVent = sattPåVentJson.erSattPåVent,
        sattPåVentBegrunnelser = sattPåVentJson.sattPåVentBegrunnelser.map { it.toSattPåVentBegrunnelse() },
    )
}

fun SattPåVent.toDbJson(): String = serialize(
    SattPåVentJson(
        erSattPåVent = erSattPåVent,
        sattPåVentBegrunnelser = sattPåVentBegrunnelser.map { it.toDbJson() },
    ),
)
