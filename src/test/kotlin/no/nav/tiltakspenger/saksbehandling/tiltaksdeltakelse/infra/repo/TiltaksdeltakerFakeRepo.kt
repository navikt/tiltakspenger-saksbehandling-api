package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.common.UlidBase.Companion.random
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

class TiltaksdeltakerFakeRepo : TiltaksdeltakerRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, String>())

    override fun hentEllerLagre(eksternId: String, sessionContext: SessionContext?): String {
        data.get()[eksternId]?.let { return it }

        val id = random(ULID_PREFIX_TILTAKSDELTAKER).toString()
        lagre(
            id = id,
            eksternId = eksternId,
        )
        return id
    }

    override fun hentInternId(eksternId: String): String? {
        return data.get()[eksternId]
    }

    override fun lagre(id: String, eksternId: String, sessionContext: SessionContext?) {
        data.get()[eksternId] = id
    }
}
