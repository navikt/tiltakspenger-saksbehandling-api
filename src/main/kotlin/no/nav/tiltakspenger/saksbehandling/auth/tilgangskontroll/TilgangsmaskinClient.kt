package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse

/**
 * Domenets grensesnitt mot Tilgangsmaskinen.
 * HTTP-implementasjonen ligger i `infra`, og porten peker aldri motsatt vei.
 */
interface TilgangsmaskinClient {
    suspend fun harTilgangTilPerson(fnr: Fnr, saksbehandlerToken: String): Either<TilgangskontrollFeil, Tilgangsvurdering>

    /**
     * Responsen returneres hel, så servicen kan logge med metadataen fra kallet når den trenger det.
     * Klienten logger ikke selv; det er servicen som kjenner saksbehandleren og correlationId-en.
     */
    suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<TilgangskontrollFeil, HttpKlientResponse<Tilgangsvurderinger>>
}
