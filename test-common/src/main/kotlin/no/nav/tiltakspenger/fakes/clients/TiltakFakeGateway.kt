package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway

class TiltakFakeGateway : TiltakGateway {
    private val data = Atomic(mutableMapOf<Fnr, List<Tiltaksdeltagelse>>())

    override suspend fun hentTiltaksdeltagelse(fnr: Fnr, maskerTiltaksnavn: Boolean, correlationId: CorrelationId): List<Tiltaksdeltagelse> {
        return data.get()[fnr]!!
    }

    fun lagre(
        fnr: Fnr,
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ) {
        data.get()[fnr] = listOf(tiltaksdeltagelse)
    }
}
