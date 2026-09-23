package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveGrunnlag
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Løsningsbegrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
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

val testbegrunnelse: Løsningsbegrunnelse =
    Løsningsbegrunnelse.Forhåndsdefinert(Løsningsbegrunnelse.Årsak.ENDRINGEN_ER_ALLEREDE_HÅNDTERT)

val testsaksbehandler: Saksbehandler = ObjectMother.saksbehandler(navIdent = "Z123456")

val annenTestsaksbehandler: Saksbehandler = ObjectMother.saksbehandler(navIdent = "Z654321")
