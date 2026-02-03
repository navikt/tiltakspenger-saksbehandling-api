package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import java.security.InvalidParameterException

private data class VentestatusDbJson(
    val ventestatusHendelser: List<VentestatusHendelseDbJson>,
)

fun String.toVentestatus(): Ventestatus {
    val ventestatusJson = try {
        deserialize<VentestatusDbJson>(this)
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }
    return Ventestatus(
        ventestatusHendelser = ventestatusJson.ventestatusHendelser.map { it.toSattPåVentBegrunnelse() },
    )
}

fun Ventestatus.toDbJson(): String = serialize(
    VentestatusDbJson(
        ventestatusHendelser = ventestatusHendelser.map { it.toDbJson() },
    ),
)
