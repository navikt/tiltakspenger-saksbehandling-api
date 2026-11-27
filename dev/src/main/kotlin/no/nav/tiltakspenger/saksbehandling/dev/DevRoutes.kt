package no.nav.tiltakspenger.saksbehandling.dev

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.hentOppdragstilstandRoute
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext

/**
 * Registrerer diagnostikkrutene som brukes i dev og lokalt.
 * Rutene registreres utenfor appens autentisering, så innlogging kreves her.
 */
fun Route.devRoutes(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        hentOppdragstilstandRoute(
            sakService = applicationContext.sakContext.sakService,
            utbetalingRepo = applicationContext.utbetalingContext.utbetalingRepo,
            auditService = applicationContext.personContext.auditService,
            tilgangskontrollService = applicationContext.tilgangskontrollService,
        )
    }
}
