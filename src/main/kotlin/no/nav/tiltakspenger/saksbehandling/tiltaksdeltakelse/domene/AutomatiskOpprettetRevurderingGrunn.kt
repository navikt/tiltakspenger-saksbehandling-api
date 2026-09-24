package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene

import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndringer

/**
 *  [hendelseId] id for kafka-hendelsen med endringene som utløste opprettelsen av revurderingen.
 *  Null når revurderingen er opprettet av [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.OppdatertTiltaksdeltakelseJobb], som ikke tolker hendelser — den henter nå-tilstanden fra tiltakshistorikk utløst av en markør på tiltaksdeltakeren.
 */
data class AutomatiskOpprettetRevurderingGrunn(
    val endringer: TiltaksdeltakerEndringer,
    val hendelseId: String?,
)
