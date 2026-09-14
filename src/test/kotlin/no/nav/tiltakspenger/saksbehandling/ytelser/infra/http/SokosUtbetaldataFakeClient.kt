package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse

/**
 * Svarer med tom liste til en test har seedet ytelser for fnr-et med [leggTilYtelser].
 *
 * Faken tar også opp oppslagene den mottar.
 * Perioden vi spør utbetaldata om, regnes ut i [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService], og lagres på saksopplysningen som `oppslagsperiode` — også ved tomt svar, siden `Ytelser.IngenTreff` bærer den.
 * Har vi derimot ikke spurt i det hele tatt, blir saksopplysningen `IkkeBehandlingsgrunnlag` uten oppslagsperiode, og da er opptaket eneste beviset på at faken ikke ble kalt.
 *
 * Merk at faken ikke filtrerer på perioden den blir spurt om.
 * En test som seeder ytelser, må selv velge en periode som rommer dem, ellers pinner den et svar utbetaldata ikke kunne gitt.
 */
class SokosUtbetaldataFakeClient : SokosUtbetaldataClient {
    private val ytelserPerFnr = Atomic(mutableMapOf<Fnr, List<Ytelse>>())
    private val mottatteOppslag = Atomic(mutableListOf<Oppslag>())

    /** Ett oppslag faken har mottatt. */
    data class Oppslag(
        val fnr: Fnr,
        val periode: Periode,
    )

    /** Oppslagene faken har mottatt, i mottatt rekkefølge. */
    val oppslag: List<Oppslag> get() = mottatteOppslag.get().toList()

    override suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<Ytelse>> {
        mottatteOppslag.get().add(Oppslag(fnr = fnr, periode = periode))
        return Either.Right(ytelserPerFnr.get()[fnr] ?: emptyList())
    }

    /** Registrerer ytelsene faken skal svare med for [fnr]. */
    fun leggTilYtelser(fnr: Fnr, ytelser: List<Ytelse>) {
        ytelserPerFnr.get()[fnr] = ytelser
    }
}
