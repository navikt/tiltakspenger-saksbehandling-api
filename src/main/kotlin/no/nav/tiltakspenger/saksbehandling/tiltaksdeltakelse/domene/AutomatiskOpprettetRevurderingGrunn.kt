package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring

data class AutomatiskOpprettetRevurderingGrunn(
    val endring: TiltaksdeltakerEndring,
)
