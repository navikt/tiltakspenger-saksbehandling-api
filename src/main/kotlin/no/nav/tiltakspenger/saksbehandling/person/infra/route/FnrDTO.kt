package no.nav.tiltakspenger.saksbehandling.person.infra.route

import no.nav.tiltakspenger.libs.common.Fnr

data class FnrDTO(
    val fnr: String,
) {
    /**
     * @throws no.nav.tiltakspenger.libs.common.UgyldigFnrException ved ugyldig fnr
     */
    fun toDomain(): Fnr = Fnr.fromString(fnr)
}
