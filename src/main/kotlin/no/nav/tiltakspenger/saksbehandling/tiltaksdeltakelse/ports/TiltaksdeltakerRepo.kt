package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.Tiltaksdeltaker

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
        sessionContext: SessionContext? = null,
    ): TiltaksdeltakerId

    // denne er primært tenkt brukt for testformål
    fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        tiltakstype: TiltakResponsDTO.TiltakTypeDTO,
        sessionContext: SessionContext? = null,
    )

    fun hentInternId(eksternId: String): TiltaksdeltakerId?

    fun hentEksternId(
        id: TiltaksdeltakerId,
        sessionContext: SessionContext?,
    ): String

    fun hentTiltaksdeltaker(eksternId: String): Tiltaksdeltaker?

    // Denne skal kun brukes når tiltaksdeltakelser flyttes ut av Arena og får ny eksternId
    fun oppdaterEksternIdForTiltaksdeltaker(
        tiltaksdeltaker: Tiltaksdeltaker,
        sessionContext: SessionContext? = null,
    )
}
