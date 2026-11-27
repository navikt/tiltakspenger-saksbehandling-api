package no.nav.tiltakspenger.saksbehandling.dev

import no.nav.tiltakspenger.saksbehandling.startIMiljø

/** Inngangspunkt for utviklingsimaget. */
fun main() {
    startDevApp(System.getenv("NAIS_CLUSTER_NAME")) { startIMiljø(tilleggsruter = { devRoutes(it) }) }
}

/**
 * Utviklingsimaget starter bare i dev-gcp, så det avviser blant annet prod før noe bruker `Configuration`.
 * [startApp] er et eget argument slik at sperren kan testes uten å starte serveren.
 */
fun startDevApp(klynge: String?, startApp: () -> Unit) {
    check(klynge == DevKonfigurasjonsleverandør.KLYNGE) {
        "Utviklingsimaget kan bare starte i ${DevKonfigurasjonsleverandør.KLYNGE}, men NAIS_CLUSTER_NAME er $klynge."
    }
    startApp()
}
