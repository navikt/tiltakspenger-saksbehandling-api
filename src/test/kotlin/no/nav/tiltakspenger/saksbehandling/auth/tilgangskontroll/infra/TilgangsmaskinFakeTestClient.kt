package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak

class TilgangsmaskinFakeTestClient : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Tilgangsvurdering>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Tilgangsvurdering {
        data.get()[fnr]?.let { return it }
        return Tilgangsvurdering.Avvist(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            status = 403,
            brukerIdent = fnr.verdi,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        )
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): TilgangBulkResponse {
        return TilgangBulkResponse(
            ansattId = "Z123456",
            resultater = fnrs.map {
                TilgangBulkResponse.TilgangResponse(
                    brukerId = it.verdi,
                    status = if (data.get()[it] == null || data.get()[it] is Tilgangsvurdering.Godkjent) {
                        204
                    } else {
                        403
                    },
                )
            },
        )
    }

    fun leggTil(
        fnr: Fnr,
        harTilgang: Tilgangsvurdering,
    ) {
        data.get()[fnr] = harTilgang
    }
}
