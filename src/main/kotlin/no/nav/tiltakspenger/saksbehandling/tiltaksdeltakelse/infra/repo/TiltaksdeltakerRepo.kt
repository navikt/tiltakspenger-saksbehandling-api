package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakType,
        sessionContext: SessionContext? = null,
    ): TiltaksdeltakerId

    // denne er primært tenkt brukt for testformål
    fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakType,
        sessionContext: SessionContext? = null,
    )

    fun hentInternId(eksternId: String): TiltaksdeltakerId?

    fun hentEksternId(
        id: TiltaksdeltakerId,
        sessionContext: SessionContext? = null,
    ): String

    fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker?

    // Denne skal kun brukes når tiltaksdeltakelser flyttes ut av Arena og får ny eksternId
    fun oppdaterEksternIdForTiltaksdeltaker(
        tiltaksdeltaker: Tiltaksdeltaker,
        sessionContext: SessionContext? = null,
    )
}
