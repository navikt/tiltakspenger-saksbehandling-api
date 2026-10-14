package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.atomic.Atomic
import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak

/**
 * Svarer med tom liste til en test har seedet vedtak for fnr-et med [leggTilVedtak].
 *
 * Merk at faken ikke filtrerer på perioden den blir spurt om.
 */
class TiltakspengerArenaFakeClient : TiltakspengerArenaClient {
    private val vedtakPerFnr = Atomic(mutableMapOf<Fnr, List<ArenaTPVedtak>>())

    override suspend fun hentTiltakspengevedtakFraArena(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<ArenaTPVedtak>> {
        return Either.Right(vedtakPerFnr.get()[fnr] ?: emptyList())
    }

    /** Registrerer vedtakene faken skal svare med for [fnr]. */
    fun leggTilVedtak(fnr: Fnr, vedtak: List<ArenaTPVedtak>) {
        vedtakPerFnr.get()[fnr] = vedtak
    }
}
