package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        sessionContext: SessionContext? = null,
    ): TiltaksdeltakerId

    // denne er primært tenkt brukt for testformål
    fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        sessionContext: SessionContext? = null,
    )

    fun hentInternId(eksternId: String): TiltaksdeltakerId?
}
