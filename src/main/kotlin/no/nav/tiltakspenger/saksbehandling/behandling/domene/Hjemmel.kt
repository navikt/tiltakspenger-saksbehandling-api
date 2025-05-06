package no.nav.tiltakspenger.saksbehandling.behandling.domene

data class Hjemmel(
    val paragraf: Paragraf,
    val forskrift: Forskrift,
    val ledd: Ledd? = null,
)
