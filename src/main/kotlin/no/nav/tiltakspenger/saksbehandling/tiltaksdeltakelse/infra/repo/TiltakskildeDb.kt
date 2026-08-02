package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde

enum class TiltakskildeDb {
    Arena,
    Komet,
    TeamTiltak,
}

fun String.toTiltakskilde(): Tiltakskilde =
    when (TiltakskildeDb.valueOf(this)) {
        TiltakskildeDb.Arena -> Tiltakskilde.Arena
        TiltakskildeDb.Komet -> Tiltakskilde.Komet
        TiltakskildeDb.TeamTiltak -> Tiltakskilde.TeamTiltak
    }

fun Tiltakskilde.toDb(): String =
    when (this) {
        Tiltakskilde.Arena -> TiltakskildeDb.Arena
        Tiltakskilde.Komet -> TiltakskildeDb.Komet
        Tiltakskilde.TeamTiltak -> TiltakskildeDb.TeamTiltak
    }.toString()
