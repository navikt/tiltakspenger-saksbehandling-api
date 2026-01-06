package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import java.util.UUID

class TiltaksdeltakerFakeRepo : TiltaksdeltakerRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, String>())

    override fun hentEllerLagre(eksternId: String, sessionContext: SessionContext?): String {
        data.get()[eksternId]?.let { return it }

        val id = UUID.randomUUID().toString()
        lagre(eksternId, id)
        return id
    }

    override fun hentInternId(eksternId: String): String? {
        return data.get()[eksternId]
    }

    fun lagre(
        eksternId: String,
        id: String,
    ) {
        data.get()[eksternId] = id
    }
}
