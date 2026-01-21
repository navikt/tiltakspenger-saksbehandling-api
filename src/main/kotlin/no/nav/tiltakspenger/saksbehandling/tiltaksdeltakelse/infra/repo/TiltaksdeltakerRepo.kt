package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.jobb.TiltaksdeltakerIdOgTiltakstype

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

    fun hentEksternId(id: TiltaksdeltakerId): String

    fun hentIdUtenTiltakstypeOgTiltakstypen(limit: Int = 75): List<TiltaksdeltakerIdOgTiltakstype>

    fun lagreTiltakstype(tiltaksdeltakerIdOgTiltakstype: TiltaksdeltakerIdOgTiltakstype)
}
