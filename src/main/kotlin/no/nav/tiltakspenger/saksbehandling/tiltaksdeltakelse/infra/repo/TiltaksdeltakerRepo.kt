package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

const val ULID_PREFIX_TILTAKSDELTAKER = "tiltaksdeltaker"

interface TiltaksdeltakerRepo {
    fun hentEllerLagre(
        eksternId: String,
        sessionContext: SessionContext? = null,
    ): String

    fun lagre(
        id: String,
        eksternId: String,
        sessionContext: SessionContext? = null,
    )

    fun hentInternId(eksternId: String): String?
}
