package no.nav.tiltakspenger.vedtak.felles.exceptions

class IkkeFunnetException(
    override val message: String,
) : RuntimeException(message)
