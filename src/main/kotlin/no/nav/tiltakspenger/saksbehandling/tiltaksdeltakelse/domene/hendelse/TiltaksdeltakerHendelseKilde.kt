package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse

/**
 * Kafka-kildene ([Arena], [TeamTiltak], [Komet]) gir hendelser som mottas.
 * [Tiltakshistorikk] er nå-tilstanden som hentes når en endring behandles.
 */
enum class TiltaksdeltakerHendelseKilde {
    Arena,
    TeamTiltak,
    Komet,
    Tiltakshistorikk,
}
