package no.nav.tiltakspenger.vedtak.felles.exceptions

class IkkeLoggDenneException(
    override val message: String,
) : RuntimeException(message)
