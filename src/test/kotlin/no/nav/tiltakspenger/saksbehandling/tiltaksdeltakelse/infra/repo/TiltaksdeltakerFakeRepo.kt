package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import java.time.LocalDateTime

class TiltaksdeltakerFakeRepo : TiltaksdeltakerRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<String, TiltaksdeltakerId>())
    private val sakIder = arrow.atomic.Atomic(mutableMapOf<TiltaksdeltakerId, SakId>())
    private val ubehandledeEndringer = arrow.atomic.Atomic(mutableMapOf<TiltaksdeltakerId, LocalDateTime>())

    override fun hentEllerLagre(
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
        sakId: SakId,
        sessionContext: SessionContext?,
    ): TiltaksdeltakerId {
        data.get()[eksternId]?.let { return it }

        val id = TiltaksdeltakerId.random()
        lagre(
            id = id,
            eksternId = eksternId,
            tiltakstype = tiltakstype,
            sakId = sakId,
        )
        return id
    }

    override fun hentInternId(eksternId: String): TiltaksdeltakerId? {
        return data.get()[eksternId]
    }

    override fun hentEksternId(id: TiltaksdeltakerId, sessionContext: SessionContext?): String {
        return data.get().filter { it.value == id }.keys.first()
    }

    override fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker? {
        return data.get()[eksternId]?.let {
            Tiltaksdeltaker(
                id = it,
                eksternId = eksternId,
                tiltakstype = TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO,
                utdatertEksternId = null,
                sakId = sakIder.get().getValue(it),
                sisteUbehandletEndringTidspunkt = ubehandledeEndringer.get()[it],
            )
        }
    }

    override fun oppdaterEksternIdForTiltaksdeltaker(
        tiltaksdeltaker: Tiltaksdeltaker,
        sessionContext: SessionContext?,
    ) {
    }

    override fun registrerUbehandletEndring(
        id: TiltaksdeltakerId,
        sakId: SakId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        sakIder.get()[id] = sakId
        ubehandledeEndringer.get()[id] = tidspunkt
    }

    override fun hentMedUbehandledeEndringer(eldreEnn: LocalDateTime): List<Tiltaksdeltaker> {
        return ubehandledeEndringer.get()
            .filter { it.value < eldreEnn }
            .toList()
            .sortedBy { it.second }
            .mapNotNull { (id, tidspunkt) ->
                val eksternId = data.get().filterValues { it == id }.keys.firstOrNull() ?: return@mapNotNull null
                Tiltaksdeltaker(
                    id = id,
                    eksternId = eksternId,
                    tiltakstype = TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO,
                    utdatertEksternId = null,
                    sakId = sakIder.get().getValue(id),
                    sisteUbehandletEndringTidspunkt = tidspunkt,
                )
            }
    }

    override fun markerEndringSomBehandlet(
        id: TiltaksdeltakerId,
        forventetSisteUbehandletEndring: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        if (ubehandledeEndringer.get()[id] == forventetSisteUbehandletEndring) {
            ubehandledeEndringer.get().remove(id)
        }
    }

    override fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
        sakId: SakId,
        sessionContext: SessionContext?,
    ) {
        data.get()[eksternId] = id
        sakIder.get()[id] = sakId
    }
}
