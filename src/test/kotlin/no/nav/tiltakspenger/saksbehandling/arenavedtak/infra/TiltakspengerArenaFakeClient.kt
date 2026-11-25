package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.atomic.Atomic
import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak

/**
 * Svarer med tom liste til en test har seedet vedtak for fnr-et med [leggTilVedtak] eller statistikk for året med [leggTilStatistikk].
 *
 * Merk at faken ikke filtrerer på perioden den blir spurt om.
 */
class TiltakspengerArenaFakeClient : TiltakspengerArenaClient {
    private val vedtakPerFnr = Atomic(mutableMapOf<Fnr, List<ArenaTPVedtak>>())
    private val statistikkPerÅr = Atomic(mutableMapOf<Int, List<ArenaStatistikkForMåned>>())

    override suspend fun hentTiltakspengevedtakFraArena(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<ArenaTPVedtak>> {
        return Either.Right(vedtakPerFnr.get()[fnr] ?: emptyList())
    }

    override suspend fun hentStatistikkPerMåned(år: Int): Either<HttpKlientError, List<ArenaStatistikkForMåned>> {
        return Either.Right(statistikkPerÅr.get()[år] ?: emptyList())
    }

    /** Registrerer statistikken faken skal svare med for [år]. */
    fun leggTilStatistikk(år: Int, statistikk: List<ArenaStatistikkForMåned>) {
        statistikkPerÅr.get()[år] = statistikk
    }

    /** Registrerer vedtakene faken skal svare med for [fnr]. */
    fun leggTilVedtak(fnr: Fnr, vedtak: List<ArenaTPVedtak>) {
        vedtakPerFnr.get()[fnr] = vedtak
    }
}
