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
        val årsak = avvistÅrsak(fnr) ?: return Tilgangsvurdering.Godkjent.right()
        return Tilgangsvurdering.Avvist(
            årsak = årsak,
            begrunnelse = "Saksbehandler har ikke tilgang til person",
            metadata = AvvistMetadata(
                type = "TilgangAvvist",
                avvisningskode = "AVVIST_$årsak",
                navIdent = "Z123456",
                brukerIdent = fnr,
            ),
        ).right()
    }

    override suspend fun harTilgangTilPersoner(
        fnrs: List<Fnr>,
        saksbehandlerToken: String,
    ): Either<Nothing, HttpKlientResponse<Map<Fnr, TilgangsvurderingBulk>>> {
        val tilgangPerFnr = fnrs.associateWith { fnr ->
            when (val årsak = avvistÅrsak(fnr)) {
                null -> TilgangsvurderingBulk.Godkjent

                else -> TilgangsvurderingBulk.Avvist(
                    årsak = årsak,
                    begrunnelse = "Saksbehandler har ikke tilgang til person",
                )
            }
        }
        return ObjectMother.httpKlientResponse(body = tilgangPerFnr, statusCode = 207).right()
    }

    /**
     * Følger samme fnr-prefiks-konvensjon som [no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient].
     * 2 gir fortrolig adresse, 3 strengt fortrolig og 4 strengt fortrolig utland — alt annet gir tilgang.
     * En eksplisitt verdi lagt inn med [leggTil] overstyrer prefikset, og avviser da med [TilgangsvurderingAvvistÅrsak.FORTROLIG].
     */
    private fun avvistÅrsak(fnr: Fnr): TilgangsvurderingAvvistÅrsak? {
        data.get()[fnr]?.let { eksplisittTilgang ->
            return if (eksplisittTilgang) null else TilgangsvurderingAvvistÅrsak.FORTROLIG
        }
        return when (fnr.verdi.first()) {
            '2' -> TilgangsvurderingAvvistÅrsak.FORTROLIG
            '3' -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG
            '4' -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND
            else -> null
        }
    }

    fun leggTil(
        fnr: Fnr,
        harTilgang: Boolean,
    ) {
        data.get()[fnr] = harTilgang
    }
}
