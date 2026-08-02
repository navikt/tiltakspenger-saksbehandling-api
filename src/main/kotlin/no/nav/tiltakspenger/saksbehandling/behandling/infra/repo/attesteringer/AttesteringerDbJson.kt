package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.attesteringer

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.Attestering

/**
 * Har ansvar for å serialisere/deserialisere Attesteringer til og fra json for lagring i database.
 */
private data class AttesteringerDbJson(
    val attesteringer: List<AttesteringDbJson>,
)

fun String.toAttesteringer(): List<Attestering> =
    deserialize<AttesteringerDbJson>(this).attesteringer.map { it.toDomain() }

fun List<Attestering>.toDbJson(): String =
    serialize(
        AttesteringerDbJson(
            attesteringer = this.map {
                it.toDbJson()
            },
        ),
    )
