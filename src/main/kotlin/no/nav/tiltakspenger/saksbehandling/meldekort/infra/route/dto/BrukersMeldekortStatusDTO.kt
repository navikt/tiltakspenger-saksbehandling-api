package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

enum class BrukersMeldekortStatusDTO {
    IKKE_MOTTATT,
    VENTER_BEHANDLING,
    KORRIGERING_VENTER_BEHANDLING,
    BEHANDLET,
    KORRIGERING_BEHANDLET,
}
