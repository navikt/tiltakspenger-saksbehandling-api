package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

/**
 * Request-body mot tiltakspenger-tiltak sitt tiltakshistorikk-endepunkt.
 * [ident] er fnr, derfor maskert `toString()`.
 */
internal data class TiltakRequestDTO(
    val ident: String,
) {
    override fun toString() = "TiltakRequestDTO(ident=*****)"
}
