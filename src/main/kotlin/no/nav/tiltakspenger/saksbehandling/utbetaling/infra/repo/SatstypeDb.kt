package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import no.nav.utsjekk.kontrakter.felles.Satstype

private enum class SatstypeDb {
    DAGLIG,
    DAGLIG_INKL_HELG,
    ;

    fun tilDomene(): Satstype = when (this) {
        DAGLIG -> Satstype.DAGLIG
        DAGLIG_INKL_HELG -> Satstype.DAGLIG_INKL_HELG
    }
}

fun Satstype.tilDb(): String {
    return when (this) {
        Satstype.DAGLIG -> SatstypeDb.DAGLIG

        Satstype.DAGLIG_INKL_HELG -> SatstypeDb.DAGLIG_INKL_HELG

        Satstype.MÅNEDLIG,
        Satstype.ENGANGS,
        -> throw IllegalArgumentException("Vi bruker ikke satstypen $this")
    }.name
}

fun String.tilSatstype(): Satstype {
    return SatstypeDb.valueOf(this).tilDomene()
}
