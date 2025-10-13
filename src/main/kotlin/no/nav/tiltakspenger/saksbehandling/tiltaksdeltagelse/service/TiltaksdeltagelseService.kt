package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.service

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient

class TiltaksdeltagelseService(
    private val sakService: SakService,
    private val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient,
) {
    suspend fun hentTiltaksdeltagelserForSak(sakId: SakId, correlationId: CorrelationId): Tiltaksdeltagelser {
        val sak = sakService.hentForSakId(sakId)
        return tiltaksdeltagelseKlient.hentTiltaksdeltagelser(sak.fnr, correlationId)
    }
}
