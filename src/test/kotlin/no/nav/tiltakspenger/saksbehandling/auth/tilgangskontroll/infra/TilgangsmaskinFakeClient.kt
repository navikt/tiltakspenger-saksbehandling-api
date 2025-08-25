package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse

class TilgangsmaskinFakeClient : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<AvvistTilgangResponse, Boolean> {
        data.get()[fnr]?.let { return it.right() }
        return AvvistTilgangResponse(
            type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
            title = "AVVIST_STRENGT_FORTROLIG_ADRESSE",
            status = 403,
            brukerIdent = fnr.verdi,
            navIdent = "Z12345",
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
        ).left()
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
                    status = if (data.get()[it] == null || data.get()[it] == true) {
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
        harTilgang: Boolean,
    ) {
        data.get()[fnr] = harTilgang
    }
}
