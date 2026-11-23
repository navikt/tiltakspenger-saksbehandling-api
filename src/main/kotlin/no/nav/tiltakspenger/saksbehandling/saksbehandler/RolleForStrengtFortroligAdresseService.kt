package no.nav.tiltakspenger.saksbehandling.saksbehandler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import java.util.UUID

/**
 * Avgjør om en Nav-ident har rollen for strengt fortrolig adresse, altså er medlem av Entra-gruppen [gruppeIdForStrengtFortroligAdresse].
 * Saksbehandlerne med rollen jobber i Vikafossen (enhet 2103), som behandler sakene der brukeren har strengt fortrolig adresse, og bruker fiktive identer.
 * Bare direkte medlemskap telles, siden entra-proxy ikke gir medlemskap gjennom nøstede grupper.
 */
class RolleForStrengtFortroligAdresseService(
    private val entraProxyClient: EntraProxyClient,
    private val gruppeIdForStrengtFortroligAdresse: UUID,
) {
    private val logger = KotlinLogging.logger {}

    /** Logger feilen selv, uten identen i vanlig logg, så kalleren bare trenger å velge hva en feil skal bety. */
    suspend fun harRolleForStrengtFortroligAdresse(navIdent: String): Either<KanIkkeHenteGrupper, Boolean> =
        entraProxyClient.hentGrupper(navIdent)
            .map { grupper -> grupper.any { it.id == gruppeIdForStrengtFortroligAdresse } }
            .onLeft { feil ->
                when (feil) {
                    is KanIkkeHenteGrupper.KallFeilet -> feil.httpKlientError.loggFeil(
                        logger = logger,
                        operasjon = "henting av grupper fra entra-proxy",
                        kontekst = "Skulle avgjøre om en saksbehandler har rollen for strengt fortrolig adresse",
                    )
                }
            }
}
