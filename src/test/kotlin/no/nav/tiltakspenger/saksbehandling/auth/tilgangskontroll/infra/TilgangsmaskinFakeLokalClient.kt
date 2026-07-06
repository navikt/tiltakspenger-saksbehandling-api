package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering

class TilgangsmaskinFakeLokalClient : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<Nothing, Tilgangsvurdering> {
        return if (harTilgang(fnr)) {
            Tilgangsvurdering.Godkjent.right()
        } else {
            Tilgangsvurdering.Avvist(
                årsak = no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak.FORTROLIG,
                begrunnelse = "Saksbehandler har ikke tilgang til person",
                metadata = AvvistMetadata(
                    type = "TilgangAvvist",
                    navIdent = "Z123456",
                    brukerIdent = fnr.verdi,
                ),
            ).right()
        }
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<Nothing, Map<Fnr, Boolean>> {
        return fnrs.associateWith { harTilgang(it) }.right()
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
