package no.nav.tiltakspenger.saksbehandling.lokal.saksbehandler.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.saksbehandler.EntraGruppe
import no.nav.tiltakspenger.saksbehandling.saksbehandler.EntraProxyClient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.KanIkkeHenteGrupper
import java.util.UUID

/**
 * Lokal erstatning for entra-proxy.
 * Alle identer er medlem av saksbehandler- og beslutter-gruppene fra den lokale konfigurasjonen, og ingen er fiktive.
 */
class EntraProxyFakeLokalClient : EntraProxyClient {
    private val grupper = setOf(
        EntraGruppe(id = UUID.fromString(Configuration.roleSaksbehandler), navn = "0000-GA-tiltakspenger-saksbehandler"),
        EntraGruppe(id = UUID.fromString(Configuration.roleBeslutter), navn = "0000-GA-tiltakspenger-beslutter"),
    )

    override suspend fun hentGrupper(navIdent: String): Either<KanIkkeHenteGrupper, Set<EntraGruppe>> = grupper.right()
}
