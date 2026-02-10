package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles

import java.lang.IllegalArgumentException

enum class Fagsystem(val kode: String) {
    AAP("AAP"),
    DAGPENGER("DP"),
    TILTAKSPENGER("TILTPENG"),
    TILLEGGSSTØNADER("TILLST"),
}

fun String.tilFagsystem(): Fagsystem =
    Fagsystem.entries.find { it.kode == this }
        ?: throw IllegalArgumentException("$this er ukjent fagsystem")
