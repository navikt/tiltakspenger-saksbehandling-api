package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import java.security.InvalidParameterException

data class VentestatusJson(
    val ventestatusHendelser: List<VentestatusHendelseJson>,
)

fun String.toVentestatus(): Ventestatus {
    val ventestatusJson = try {
        deserialize<VentestatusJson>(this)
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }
    return Ventestatus(
        ventestatusHendelser = ventestatusJson.ventestatusHendelser.map { it.toSattPåVentBegrunnelse() },
    )
}

fun Ventestatus.toDbJson(): String = serialize(
    VentestatusJson(
        ventestatusHendelser = ventestatusHendelser.map { it.toDbJson() },
    ),
)
