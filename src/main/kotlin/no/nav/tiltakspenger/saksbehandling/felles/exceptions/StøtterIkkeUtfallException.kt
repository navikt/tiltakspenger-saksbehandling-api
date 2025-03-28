package no.nav.tiltakspenger.saksbehandling.felles.exceptions

class StøtterIkkeUtfallException(
    override val message: String,
) : RuntimeException(message)
