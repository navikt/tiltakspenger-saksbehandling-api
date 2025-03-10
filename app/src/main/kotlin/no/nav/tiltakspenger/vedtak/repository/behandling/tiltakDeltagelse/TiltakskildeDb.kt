package no.nav.tiltakspenger.vedtak.repository.behandling.tiltakDeltagelse

import no.nav.tiltakspenger.vedtak.saksbehandling.domene.tiltak.Tiltakskilde

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
