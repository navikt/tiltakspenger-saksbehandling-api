package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.jobb.TiltaksdeltakerEndringer

/**
 *  [hendelseId] id for kafka-hendelsen med endringene som utløste opprettelsen av revurderingen
 */
data class AutomatiskOpprettetRevurderingGrunn(
    val endringer: TiltaksdeltakerEndringer,
    val hendelseId: String,
)
