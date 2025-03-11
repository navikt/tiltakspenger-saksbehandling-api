package no.nav.tiltakspenger.saksbehandling.felles.exceptions

class IkkeFunnetException(
    override val message: String,
) : RuntimeException(message)
