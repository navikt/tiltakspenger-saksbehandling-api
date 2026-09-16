package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

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
                årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
                begrunnelse = "Saksbehandler har ikke tilgang til person",
                metadata = AvvistMetadata(
                    type = "TilgangAvvist",
                    avvisningskode = "AVVIST_FORTROLIG_ADRESSE",
                    navIdent = "Z123456",
                    brukerIdent = fnr,
                ),
            ).right()
        }
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<Nothing, HttpKlientResponse<Map<Fnr, TilgangsvurderingBulk>>> {
        val tilgangPerFnr = fnrs.associateWith {
            if (harTilgang(it)) {
                TilgangsvurderingBulk.Godkjent
            } else {
                TilgangsvurderingBulk.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
                    begrunnelse = "Saksbehandler har ikke tilgang til person",
                )
            }
        }
        return ObjectMother.httpKlientResponse(body = tilgangPerFnr, statusCode = 207).right()
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
