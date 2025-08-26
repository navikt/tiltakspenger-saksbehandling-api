package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistTilgangResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangBulkResponse

class TilgangsmaskinFakeLokalClient : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<AvvistTilgangResponse, Boolean> {
        return harTilgang(fnr).right()
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
