package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering
import java.security.InvalidParameterException

/**
 * Har ansvar for å serialisere/deserialisere Attesteringer til og fra json for lagring i database.
 */
private data class AttesteringerDbJson(
    val attesteringer: List<AttesteringDbJson>,
)

internal fun String.toAttesteringer(): List<no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering> {
    try {
        val attesteringerDbJson = deserialize<AttesteringerDbJson>(this)
        return attesteringerDbJson.attesteringer.map {
            it.toDomain()
        }
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }
}

internal fun List<no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Attestering>.toDbJson(): String =
    serialize(
        AttesteringerDbJson(
            attesteringer = this.map {
                it.toDbJson()
            },
        ),
    )
