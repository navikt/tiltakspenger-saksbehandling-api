package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.atomic.Atomic
import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaUtbetalingerForMåned
import java.time.LocalDate

/**
 * Svarer med tom liste til en test har seedet vedtak for fnr-et med [leggTilVedtak] eller statistikk for året med [leggTilStatistikk].
 * Svarer med en tom fordeling på siste dag med rett til en test har satt fordelingen med [settSisteDagMedRett].
 * Svarer med tom liste til en test har seedet utbetalinger for året med [leggTilUtbetalinger].
 *
 * Merk at faken ikke filtrerer på perioden den blir spurt om.
 */
class TiltakspengerArenaFakeClient : TiltakspengerArenaClient {
    private val vedtakPerFnr = Atomic(mutableMapOf<Fnr, List<ArenaTPVedtak>>())
    private val statistikkPerÅr = Atomic(mutableMapOf<Int, List<ArenaStatistikkForMåned>>())
    private val sisteDagMedRett = Atomic<ArenaSisteDagMedRettEtter?>(null)
    private val utbetalingerPerÅr = Atomic(mutableMapOf<Int, List<ArenaUtbetalingerForMåned>>())

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

    override suspend fun hentSisteDagMedRettEtter(etter: LocalDate): Either<HttpKlientError, ArenaSisteDagMedRettEtter> {
        return Either.Right(sisteDagMedRett.get() ?: ArenaSisteDagMedRettEtter(etter, emptyList()))
    }

    override suspend fun hentUtbetalingerPerMåned(år: Int): Either<HttpKlientError, List<ArenaUtbetalingerForMåned>> {
        return Either.Right(utbetalingerPerÅr.get()[år] ?: emptyList())
    }

    /** Registrerer utbetalingene faken skal svare med for [år]. */
    fun leggTilUtbetalinger(år: Int, utbetalinger: List<ArenaUtbetalingerForMåned>) {
        utbetalingerPerÅr.get()[år] = utbetalinger
    }

    /** Setter fordelingen faken skal svare med, uansett hvilken dato den blir spurt om. */
    fun settSisteDagMedRett(fordeling: ArenaSisteDagMedRettEtter) {
        sisteDagMedRett.set(fordeling)
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
