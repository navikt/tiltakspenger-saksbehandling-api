package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

class TiltaksdeltakerFakeRepo : TiltaksdeltakerRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, TiltaksdeltakerId>())

    override fun hentEllerLagre(
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakType,
        sessionContext: SessionContext?,
    ): TiltaksdeltakerId {
        data.get()[eksternId]?.let { return it }

        val id = TiltaksdeltakerId.random()
        lagre(
            id = id,
            eksternId = eksternId,
            tiltakstype = tiltakstype,
        )
        return id
    }

    override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
        return data.get()[eksternId]
    }

    override fun hentEksternId(id: TiltaksdeltakerId): String {
        return data.get().filter { it.value == id }.keys.first()
    }

    override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
        return data.get()[eksternId]?.let {
            Tiltaksdeltaker(
                id = it,
                eksternId = eksternId,
                tiltakstype = TiltakResponsDTO.TiltakType.GRUPPEAMO,
                utdatertEksternId = null,
            )
        }
    }

    override fun oppdaterEksternIdForTiltaksdeltaker(
        tiltaksdeltaker: Tiltaksdeltaker,
        sessionContext: SessionContext?,
    ) {
    }

    override fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakType,
        sessionContext: SessionContext?,
    ) {
        data.get()[eksternId] = id
    }
}
