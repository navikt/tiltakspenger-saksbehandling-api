package no.nav.tiltakspenger.saksbehandling.dagpenger.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode

/**
 * Svarer med tom liste til en test har seedet dagpengeperioder for fnr-et med [leggTilDagpengeperioder].
 *
 * Merk at faken ikke filtrerer på perioden den blir spurt om.
 */
class DagpengerFakeKlient : DagpengerKlient {
    private val perioderPerFnr = Atomic(mutableMapOf<Fnr, List<Dagpengeperiode>>())

    override suspend fun hentDagpengeperioder(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<Dagpengeperiode>> {
        return Either.Right(perioderPerFnr.get()[fnr] ?: emptyList())
    }

    /** Registrerer dagpengeperiodene faken skal svare med for [fnr]. */
    fun leggTilDagpengeperioder(fnr: Fnr, perioder: List<Dagpengeperiode>) {
        perioderPerFnr.get()[fnr] = perioder
    }
}
