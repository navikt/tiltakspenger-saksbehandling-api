package no.nav.tiltakspenger.saksbehandling.saksbehandler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.util.concurrent.ConcurrentHashMap

/** Gir ingen grupper for identer som ikke er lagt inn med [leggTil], og feiler for identer lagt inn med [feilFor]. */
class EntraProxyFakeClient : EntraProxyClient {
    private val grupper = ConcurrentHashMap<String, Set<EntraGruppe>>()
    private val feil = ConcurrentHashMap<String, KanIkkeHenteGrupper>()

    fun leggTil(navIdent: String, vararg grupper: EntraGruppe) {
        this.grupper[navIdent] = grupper.toSet()
    }

    fun feilFor(navIdent: String, feil: KanIkkeHenteGrupper) {
        this.feil[navIdent] = feil
    }

    override suspend fun hentGrupper(navIdent: String): Either<KanIkkeHenteGrupper, Set<EntraGruppe>> =
        feil[navIdent]?.left() ?: (grupper[navIdent] ?: emptySet()).right()
}
