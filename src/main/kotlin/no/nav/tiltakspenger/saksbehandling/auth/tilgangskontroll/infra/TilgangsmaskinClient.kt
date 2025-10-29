package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering

interface TilgangsmaskinClient {
    suspend fun harTilgangTilPerson(fnr: Fnr, saksbehandlerToken: String): Tilgangsvurdering
    suspend fun harTilgangTilPersoner(fnrs: List<Fnr>, saksbehandlerToken: String): TilgangBulkResponse
}
