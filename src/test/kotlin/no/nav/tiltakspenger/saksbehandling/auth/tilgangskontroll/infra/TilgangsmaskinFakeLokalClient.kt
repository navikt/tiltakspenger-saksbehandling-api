package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsmaskinClient
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurderinger
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient

/**
 * [tokenerMedFullTilgang] er lokale saksbehandlere som har tilgang til fortrolig og strengt fortrolig adresse, slik saksbehandlere med de gruppene har i prod.
 */
class TilgangsmaskinFakeLokalClient(
    private val personFakeKlient: PersonFakeKlient,
    private val tokenerMedFullTilgang: Set<String>,
) : TilgangsmaskinClient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun harTilgangTilPerson(
        fnr: Fnr,
        saksbehandlerToken: String,
    ): Either<Nothing, Tilgangsvurdering> {
        val årsak = avvistÅrsak(fnr, saksbehandlerToken) ?: return Tilgangsvurdering.Godkjent.right()
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
    ): Either<Nothing, HttpKlientResponse<Tilgangsvurderinger>> {
        val tilgangPerFnr = fnrs.associateWith { fnr ->
            when (val årsak = avvistÅrsak(fnr, saksbehandlerToken)) {
                null -> TilgangsvurderingBulk.Godkjent

                else -> TilgangsvurderingBulk.Avvist(
                    årsak = årsak,
                    begrunnelse = "Saksbehandler har ikke tilgang til person",
                )
            }
        }
        return ObjectMother.httpKlientResponse(
            body = Tilgangsvurderinger(perFnr = tilgangPerFnr, ukjenteAvvisningskoder = emptySet()),
            statusCode = 207,
        ).right()
    }

    /**
     * Adressebeskyttelsen hentes fra [PersonFakeKlient], som eier fnr-prefiks-konvensjonen.
     * En eksplisitt verdi lagt inn med [leggTil] overstyrer personen, og avviser da med [TilgangsvurderingAvvistÅrsak.FORTROLIG].
     */
    private suspend fun avvistÅrsak(fnr: Fnr, saksbehandlerToken: String): TilgangsvurderingAvvistÅrsak? {
        if (saksbehandlerToken in tokenerMedFullTilgang) return null
        data.get()[fnr]?.let { eksplisittTilgang ->
            return if (eksplisittTilgang) null else TilgangsvurderingAvvistÅrsak.FORTROLIG
        }
        val adressebeskyttelse = personFakeKlient.hentAdressebeskyttelse(listOf(fnr)).getOrThrow()[fnr]!!
        return when (adressebeskyttelse) {
            Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND
            Adressebeskyttelse.STRENGT_FORTROLIG -> TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG
            Adressebeskyttelse.FORTROLIG -> TilgangsvurderingAvvistÅrsak.FORTROLIG
            Adressebeskyttelse.UGRADERT -> null
        }
    }

    fun leggTil(
        fnr: Fnr,
        harTilgang: Boolean,
    ) {
        data.get()[fnr] = harTilgang
    }
}
