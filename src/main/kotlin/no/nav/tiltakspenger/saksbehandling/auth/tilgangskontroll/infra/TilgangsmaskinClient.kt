package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollFeil
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering

interface TilgangsmaskinClient {
    suspend fun harTilgangTilPerson(fnr: Fnr, saksbehandlerToken: String): Either<TilgangskontrollFeil, Tilgangsvurdering>

    suspend fun harTilgangTilPersoner(fnrs: List<Fnr>, saksbehandlerToken: String): Either<TilgangskontrollFeil, Map<Fnr, Boolean>>
}
