package no.nav.tiltakspenger.saksbehandling.vedtak

/**
 * 0, 1 eller flere lovlige kommandoer man kan utføre på et rammevedtak.
 */
data class Rammevedtakskommandoer(
    val value: Set<Rammevedtakskommando>,
) : Set<Rammevedtakskommando> by value
