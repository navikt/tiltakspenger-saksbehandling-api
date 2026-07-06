package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak

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
                navIdent = "Z12345",
                brukerIdent = fnr.verdi,
            ),
        ).right()
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<Nothing, Map<Fnr, Boolean>> {
        return fnrs.associateWith {
            data.get()[it] == null || data.get()[it] is Tilgangsvurdering.Godkjent
        }.right()
    }

    fun leggTil(
        fnr: Fnr,
        harTilgang: Tilgangsvurdering,
    ) {
        data.get()[fnr] = harTilgang
    }
}
