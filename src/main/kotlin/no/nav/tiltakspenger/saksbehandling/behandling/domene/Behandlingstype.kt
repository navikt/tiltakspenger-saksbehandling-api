package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * https://kodeverk.ansatt.nav.no/kodeverk/Behandlingstyper
 *
 * ae0245 Førstegangssøknad
 * ae0034 Søknad
 * ae0032 Stans
 * ae0047 Gjenopptak
 * ae0028 Revurdering
 * ae0058 Klage
 */
enum class Behandlingstype {
    SØKNADSBEHANDLING,
    REVURDERING,
}
