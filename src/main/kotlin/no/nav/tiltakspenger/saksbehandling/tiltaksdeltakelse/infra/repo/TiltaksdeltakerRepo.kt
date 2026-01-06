package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        sessionContext: SessionContext? = null,
    ): String

    fun hentInternId(eksternId: String): String?
}
