package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForAvslag

internal fun Set<ValgtHjemmelForAvslag>.toDb(): String = serialize(this.map { it.toDb() })

internal fun String.toAvslagsgrunner(): Set<ValgtHjemmelForAvslag> {
    return deserializeList<ValgtHjemmelHarIkkeRettighetDb>(this).map {
        // liten hack for å få riktig type. Kaster exception dersom it ikke er av type ValgtHjemmelForAvslag
        it.toDomain() as ValgtHjemmelForAvslag
    }.toSet()
}
