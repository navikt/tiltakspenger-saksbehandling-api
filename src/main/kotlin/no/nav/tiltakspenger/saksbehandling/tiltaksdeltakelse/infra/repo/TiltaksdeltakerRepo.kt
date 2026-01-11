package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        internIdHvisMangler: TiltaksdeltakerId = TiltaksdeltakerId.random(),
        sessionContext: SessionContext? = null,
    ): TiltaksdeltakerId

    fun lagre(
        id: TiltaksdeltakerId,
        eksternId: String,
        sessionContext: SessionContext? = null,
    )

    fun hentInternId(eksternId: String): TiltaksdeltakerId?
}
