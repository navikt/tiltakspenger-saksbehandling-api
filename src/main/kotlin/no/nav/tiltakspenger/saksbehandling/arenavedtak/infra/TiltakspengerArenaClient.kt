package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

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

interface TiltakspengerArenaClient {
    suspend fun hentTiltakspengevedtakFraArena(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<ArenaTPVedtak>>

    suspend fun hentStatistikkPerMåned(år: Int): Either<HttpKlientError, List<ArenaStatistikkForMåned>>

    suspend fun hentSisteDagMedRettEtter(etter: LocalDate): Either<HttpKlientError, ArenaSisteDagMedRettEtter>

    suspend fun hentUtbetalingerPerMåned(år: Int): Either<HttpKlientError, List<ArenaUtbetalingerForMåned>>
}
