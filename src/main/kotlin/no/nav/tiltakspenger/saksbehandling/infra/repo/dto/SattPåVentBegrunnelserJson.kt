package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.SattPåVentBegrunnelse
import java.security.InvalidParameterException

private data class SattPåVentBegrunnelserJson(
    val sattPåVentBegrunnelser: List<SattPåVentBegrunnelseJson>,
)

internal fun String.toSattPåVentBegrunnelser(): List<SattPåVentBegrunnelse> {
    try {
        val sattPåVentBegrunnelserJson = deserialize<SattPåVentBegrunnelserJson>(this)
        return sattPåVentBegrunnelserJson.sattPåVentBegrunnelser.map {
            it.toSattPåVentBegrunnelse()
        }
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }
}

internal fun List<SattPåVentBegrunnelse>.toDbJson(): String =
    serialize(
        SattPåVentBegrunnelserJson(
            sattPåVentBegrunnelser = this.map {
                it.toDbJson()
            },
        ),
    )
