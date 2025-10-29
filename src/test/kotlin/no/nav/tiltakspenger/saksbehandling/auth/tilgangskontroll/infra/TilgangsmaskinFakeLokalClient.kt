package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering

class TilgangsmaskinFakeLokalClient : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Tilgangsvurdering {
        return if (harTilgang(fnr)) {
            Tilgangsvurdering.Godkjent
        } else {
            Tilgangsvurdering.Avvist(
                type = "TilgangAvvist",
                årsak = no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak.FORTROLIG,
                status = 403,
                brukerIdent = fnr.verdi,
                navIdent = "Z123456",
                begrunnelse = "Saksbehandler har ikke tilgang til person",
            )
        }
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
                    status = if (harTilgang(it)) {
                        204
                    } else {
                        403
                    },
                )
            },
        )
    }

    private fun harTilgang(fnr: Fnr): Boolean {
        return data.get()[fnr] == null || data.get()[fnr] == true
    }

    fun leggTil(
        fnr: Fnr,
        harTilgang: Boolean,
    ) {
        data.get()[fnr] = harTilgang
    }
}
