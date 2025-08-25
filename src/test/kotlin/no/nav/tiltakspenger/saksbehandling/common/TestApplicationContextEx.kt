package no.nav.tiltakspenger.saksbehandling.common

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication

fun withTestApplicationContext(
    additionalConfig: Application.() -> Unit = {},
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContext) -> Unit,
) = with(TestApplicationContext()) {
    testApplication {
        application {
            jacksonSerialization()
            configureExceptions()
            setupAuthentication(texasClient)
            routing { routes(this@with) }
            additionalConfig()
        }
        testBlock(this@with)
    }
}
