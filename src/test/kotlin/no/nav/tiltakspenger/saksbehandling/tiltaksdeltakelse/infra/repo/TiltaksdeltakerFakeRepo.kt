package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

class TiltaksdeltakerFakeRepo : TiltaksdeltakerRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, TiltaksdeltakerId>())

    override fun hentEllerLagre(eksternId: String, sessionContext: SessionContext?): TiltaksdeltakerId {
        data.get()[eksternId]?.let { return it }

        val id = TiltaksdeltakerId.random()
        lagre(
            id = id,
            eksternId = eksternId,
        )
        return id
    }

    override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
        return data.get()[eksternId]
    }

    override fun lagre(id: TiltaksdeltakerId, eksternId: String, sessionContext: SessionContext?) {
        data.get()[eksternId] = id
    }
}
