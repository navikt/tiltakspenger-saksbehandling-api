package no.nav.tiltakspenger.vedtak.felles.exceptions

class StøtterIkkeUtfallException(
    override val message: String,
) : RuntimeException(message)
