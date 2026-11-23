package no.nav.tiltakspenger.saksbehandling.saksbehandler

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import java.util.UUID

/**
 * Port for oppslag av ansattdata i Entra via entra-proxy.
 * Implementeres av [no.nav.tiltakspenger.saksbehandling.saksbehandler.infra.EntraProxyHttpClient].
 */
interface EntraProxyClient {
    /**
     * Gruppene [navIdent] er direkte medlem av.
     * Bare sikkerhetsgrupper uten e-post (gruppetypen «Security» i Entra) er med, og medlemskap gjennom nøstede grupper kommer ikke med.
     * En ident entra-proxy ikke finner i Entra, gir et tomt sett.
     */
    suspend fun hentGrupper(navIdent: String): Either<KanIkkeHenteGrupper, Set<EntraGruppe>>
}

data class EntraGruppe(
    val id: UUID,
    val navn: String,
)

/** Mulige feil ved henting av gruppene til en ansatt fra entra-proxy. */
sealed interface KanIkkeHenteGrupper {
    /**
     * Selve HTTP-kallet feilet (timeout/IO/feil ved token-henting/uventet status/deserialisering osv.).
     * Se [httpKlientError] for detaljer.
     */
    data class KallFeilet(
        val httpKlientError: HttpKlientError,
    ) : KanIkkeHenteGrupper
}
