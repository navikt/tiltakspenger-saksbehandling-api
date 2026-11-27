package no.nav.tiltakspenger.saksbehandling.dev

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.oppdragstilstand.hentOppdragstilstandRoute
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.saksoppslag.utviklingstilgangForSaksoppslag
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.startIMiljø

val registrerUtviklingsruter: Route.(ApplicationContext) -> Unit = { context ->
    hentOppdragstilstandRoute(
        sakService = context.sakContext.sakService,
        utbetalingRepo = context.utbetalingContext.utbetalingRepo,
        auditService = context.personContext.auditService,
        tilgangskontrollService = context.tilgangskontrollService,
        tillatMaskintoken = true,
    )
}

/** Inngangspunkt for utviklingsimaget. */
fun main() {
    startDevApp(System.getenv("NAIS_CLUSTER_NAME")) {
        startIMiljø(
            opprettSaksoppslagstilgang = ::utviklingstilgangForSaksoppslag,
            tilleggsruter = registrerUtviklingsruter,
        )
    }
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
