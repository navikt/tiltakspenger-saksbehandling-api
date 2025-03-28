package no.nav.tiltakspenger.saksbehandling.felles.exceptions

class IkkeLoggDenneException(
    override val message: String,
) : RuntimeException(message)
