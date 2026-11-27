package no.nav.tiltakspenger.saksbehandling.lokal

import no.nav.tiltakspenger.saksbehandling.startIMiljø

/**
 * Inngangspunktet for imaget docker-compose i metarepoet bygger.
 * Kjører den vanlige oppstarten med [no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext], [LocalConfig] og ekte klienter mot tjenestene i docker-compose, og lytter på 0.0.0.0.
 * [main] i `LokalMain.kt` er den andre lokale flyten, med fakes og bare 127.0.0.1.
 */
fun main() {
    krevLokalKlynge(System.getenv("NAIS_CLUSTER_NAME"))
    startIMiljø()
}
