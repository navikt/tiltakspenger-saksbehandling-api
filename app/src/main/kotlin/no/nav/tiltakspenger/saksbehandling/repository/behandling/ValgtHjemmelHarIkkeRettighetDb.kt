package no.nav.tiltakspenger.saksbehandling.repository.behandling

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForAvslag
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelType

/**
 * @see ValgtHjemmelHarIkkeRettighet
 */

class ValgtHjemmelHarIkkeRettighetDb(
    val kode: String,
    val type: ValgtHjemmelTypeDb,
)

fun List<ValgtHjemmelHarIkkeRettighet>.toDbJson(): String {
    return this.map {
        ValgtHjemmelHarIkkeRettighetDb(
            kode = it.kode,
            type = it.type.toDb(),
        )
    }.let { serialize(it) }
}

fun String.toValgtHjemmelHarIkkeRettighet(): List<ValgtHjemmelHarIkkeRettighet> {
    return deserializeList<ValgtHjemmelHarIkkeRettighetDb>(this)
        .map {
            when (it.type) {
                ValgtHjemmelTypeDb.STANS -> ValgtHjemmelForStans::class.sealedSubclasses
                ValgtHjemmelTypeDb.AVSLAG -> ValgtHjemmelForAvslag::class.sealedSubclasses
            }.first { subclass -> subclass.objectInstance?.kode == it.kode }
                .objectInstance!!
        }
}

enum class ValgtHjemmelTypeDb {
    STANS,
    AVSLAG,
}

fun ValgtHjemmelType.toDb() =
    when (this) {
        ValgtHjemmelType.STANS -> ValgtHjemmelTypeDb.STANS
        ValgtHjemmelType.AVSLAG -> ValgtHjemmelTypeDb.AVSLAG
    }
