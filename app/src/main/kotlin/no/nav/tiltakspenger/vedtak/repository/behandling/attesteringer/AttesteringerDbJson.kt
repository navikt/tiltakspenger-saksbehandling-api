package no.nav.tiltakspenger.vedtak.repository.behandling.attesteringer

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Attestering
import java.security.InvalidParameterException

/**
 * Har ansvar for å serialisere/deserialisere Attesteringer til og fra json for lagring i database.
 */
private data class AttesteringerDbJson(
    val attesteringer: List<AttesteringDbJson>,
)

internal fun String.toAttesteringer(): List<Attestering> {
    try {
        val attesteringerDbJson = deserialize<AttesteringerDbJson>(this)
        return attesteringerDbJson.attesteringer.map {
            it.toDomain()
        }
    } catch (exception: Exception) {
        throw InvalidParameterException("Det oppstod en feil ved parsing av json: " + exception.message)
    }
}

internal fun List<Attestering>.toDbJson(): String =
    serialize(
        AttesteringerDbJson(
            attesteringer = this.map {
                it.toDbJson()
            },

        ),
    )
