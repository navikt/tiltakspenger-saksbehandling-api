package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndringer

data class AutomatiskOpprettetRevurderingGrunn(
    val endringer: TiltaksdeltakerEndringer,
    val hendelseId: String,
)
