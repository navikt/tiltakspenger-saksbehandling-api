package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Fødselsnumre
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.tilUtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.SokosUtbetaldataClient
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.YtelserMedMetadata

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
        val identer: NonEmptyList<Fnr>,
        val periode: Periode,
    )

    /** Oppslagene faken har mottatt, i mottatt rekkefølge. */
    val oppslag: List<Oppslag> get() = mottatteOppslag.get().toList()

    override suspend fun hentYtelserFraUtbetaldata(
        fødselsnumre: Fødselsnumre,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, YtelserMedMetadata> {
        mottatteOppslag.get().add(Oppslag(identer = fødselsnumre.alle, periode = periode))
        return Either.Right(
            YtelserMedMetadata(
                ytelser = fødselsnumre.alle.toList().flatMap { ytelserPerFnr.get()[it] ?: emptyList() },
                metadata = fødselsnumre.alle.map { ObjectMother.httpKlientResponse(body = Unit).metadata.tilUtbetalingsoversiktMetadata(CorrelationId.generate()) },
            ),
        )
    }

    /** Registrerer ytelsene faken skal svare med for [fnr]. */
    fun leggTilYtelser(fnr: Fnr, ytelser: List<Ytelse>) {
        ytelserPerFnr.get()[fnr] = ytelser
    }
}
