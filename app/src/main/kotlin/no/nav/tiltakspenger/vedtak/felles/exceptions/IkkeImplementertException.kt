package no.nav.tiltakspenger.vedtak.felles.exceptions

class IkkeImplementertException(
    override val message: String,
) : RuntimeException(message)
