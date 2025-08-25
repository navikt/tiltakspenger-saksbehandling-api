package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse

interface TilgangsmaskinClient {
    suspend fun harTilgangTilPerson(fnr: Fnr, saksbehandlerToken: String): Either<AvvistTilgangResponse, Boolean>
    suspend fun harTilgangTilPersoner(fnrs: List<Fnr>, saksbehandlerToken: String): TilgangBulkResponse
}
