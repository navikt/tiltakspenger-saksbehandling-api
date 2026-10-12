package no.nav.tiltakspenger.saksbehandling.lokal.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.lokal.LocalApplicationContext
import no.nav.tiltakspenger.saksbehandling.lokal.klage.infra.route.nyKlagehendelseDevRoute
import no.nav.tiltakspenger.saksbehandling.lokal.søknad.infra.route.nySøknadDevRoute
import no.nav.tiltakspenger.saksbehandling.lokal.tiltaksdeltakelse.infra.route.oppdaterTiltaksdeltakelseDevRoute

const val DEV_ROUTE = "/dev"

fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    val applicationContext = applicationContext as LocalApplicationContext

    nySøknadDevRoute(applicationContext = applicationContext)
    nyKlagehendelseDevRoute(lokalKabalHendelseService = applicationContext.lokalKabalHendelseService)
    oppdaterTiltaksdeltakelseDevRoute(lokalTiltaksdeltakelseService = applicationContext.lokalTiltaksdeltakelseService)
}
