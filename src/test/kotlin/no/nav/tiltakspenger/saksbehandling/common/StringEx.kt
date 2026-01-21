package no.nav.tiltakspenger.saksbehandling.common

fun String.medQuotes(): String {
    return this.let { "\"$this\"" }
}
