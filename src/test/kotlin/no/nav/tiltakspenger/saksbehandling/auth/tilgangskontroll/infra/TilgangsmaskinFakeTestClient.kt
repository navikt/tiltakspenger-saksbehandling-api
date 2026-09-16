package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangskontrollFeil
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

open class TilgangsmaskinFakeTestClient : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Tilgangsvurdering>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<Nothing, Tilgangsvurdering> {
        return data.get()[fnr]?.let { it.right() } ?: Tilgangsvurdering.Avvist(
            årsak = TilgangsvurderingAvvistÅrsak.FORTROLIG,
            begrunnelse = "Du har ikke tilgang til brukere med strengt fortrolig adresse",
            metadata = AvvistMetadata(
                type = "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                avvisningskode = "AVVIST_FORTROLIG_ADRESSE",
                navIdent = "Z12345",
                brukerIdent = fnr,
            ),
        ).right()
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<TilgangskontrollFeil, HttpKlientResponse<Map<Fnr, TilgangsvurderingBulk>>> {
        val tilgangPerFnr = fnrs.associateWith {
            when (val vurdering = data.get()[it]) {
                null, Tilgangsvurdering.Godkjent -> TilgangsvurderingBulk.Godkjent

                is Tilgangsvurdering.Avvist -> TilgangsvurderingBulk.Avvist(
                    årsak = vurdering.årsak,
                    begrunnelse = vurdering.begrunnelse,
                )
            }
        }
        return ObjectMother.httpKlientResponse(body = tilgangPerFnr, statusCode = 207).right()
    }

    fun leggTil(
        fnr: Fnr,
        harTilgang: Tilgangsvurdering,
    ) {
        data.get()[fnr] = harTilgang
    }
}
