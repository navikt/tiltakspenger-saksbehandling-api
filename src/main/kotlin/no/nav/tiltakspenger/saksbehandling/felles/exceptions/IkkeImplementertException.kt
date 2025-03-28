package no.nav.tiltakspenger.saksbehandling.felles.exceptions

class IkkeImplementertException(
    override val message: String,
) : RuntimeException(message)
