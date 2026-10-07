package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring

/**
 * @param tiltaksdeltakerId Deltakelsen endringen gjelder.
 * Er null for revurderinger som ble opprettet før feltet ble lagt til.
 */
data class AutomatiskOpprettetRevurderingGrunn(
    val endring: TiltaksdeltakerEndring,
    val tiltaksdeltakerId: TiltaksdeltakerId?,
)
