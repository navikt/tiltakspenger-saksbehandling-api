package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak

class TiltakspengerArenaFakeClient : TiltakspengerArenaClient {
    override suspend fun hentTiltakspengevedtakFraArena(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): List<ArenaTPVedtak> {
        return emptyList()
    }
}
