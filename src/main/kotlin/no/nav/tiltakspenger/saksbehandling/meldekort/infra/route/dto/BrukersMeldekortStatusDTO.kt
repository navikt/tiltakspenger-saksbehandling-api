package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

enum class BrukersMeldekortStatusDTO {
    IKKE_MOTTATT,
    VENTER_BEHANDLING,
    KORRIGERING_VENTER_BEHANDLING,
    UNDER_BEHANDLING,
    KORRIGERING_UNDER_BEHANDLING,
    BEHANDLET,
    KORRIGERING_BEHANDLET,
    AVBRUTT,
    KORRIGERING_AVBRUTT,
}
