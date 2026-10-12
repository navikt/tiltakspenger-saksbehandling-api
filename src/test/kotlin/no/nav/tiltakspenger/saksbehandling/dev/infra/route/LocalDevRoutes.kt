package no.nav.tiltakspenger.saksbehandling.dev.infra.route

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.saksbehandling.LocalApplicationContext
import no.nav.tiltakspenger.saksbehandling.dev.klage.infra.route.nyKlagehendelseDevRoute
import no.nav.tiltakspenger.saksbehandling.dev.søknad.infra.route.nySøknadDevRoute
import no.nav.tiltakspenger.saksbehandling.dev.tiltaksdeltakelse.infra.route.oppdaterTiltaksdeltakelseDevRoute
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext

const val DEV_ROUTE = "/dev"

fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    val applicationContext = applicationContext as LocalApplicationContext

    nySøknadDevRoute(applicationContext = applicationContext)
    nyKlagehendelseDevRoute(lokalKabalHendelseService = applicationContext.lokalKabalHendelseService)
    oppdaterTiltaksdeltakelseDevRoute(lokalTiltaksdeltakelseService = applicationContext.lokalTiltaksdeltakelseService)
}
