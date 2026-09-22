package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveGrunnlag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId

fun endretTiltaksdeltakelseGrunnlag(
    tiltaksdeltakerId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    hendelseId: TiltaksdeltakerHendelseId = TiltaksdeltakerHendelseId.random(),
    beskrivelse: String = "Deltakelsen har fått en ny sluttdato.",
): InternOppgaveGrunnlag.EndretTiltaksdeltakelse = InternOppgaveGrunnlag.EndretTiltaksdeltakelse(
    tiltaksdeltakerId = tiltaksdeltakerId,
    hendelseId = hendelseId,
    beskrivelse = beskrivelse,
)
