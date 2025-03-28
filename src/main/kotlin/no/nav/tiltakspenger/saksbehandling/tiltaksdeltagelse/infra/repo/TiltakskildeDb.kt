package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltakskilde

enum class TiltakskildeDb {
    Arena,
    Komet,
}

internal fun String.toTiltakskilde(): Tiltakskilde =
    when (TiltakskildeDb.valueOf(this)) {
        TiltakskildeDb.Arena -> Tiltakskilde.Arena
        TiltakskildeDb.Komet -> Tiltakskilde.Komet
    }

internal fun Tiltakskilde.toDb(): String =
    when (this) {
        Tiltakskilde.Arena -> TiltakskildeDb.Arena
        Tiltakskilde.Komet -> TiltakskildeDb.Komet
    }.toString()
