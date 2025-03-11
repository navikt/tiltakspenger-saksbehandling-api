package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

/**
 * https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/Behandlingstyper
 *
 * ae0245 Førstegangssøknad
 * ae0034 Søknad
 * ae0032 Stans
 * ae0047 Gjenopptak
 * ae0028 Revurdering
 * ae0058 Klage
 */
enum class Behandlingstype {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
}
