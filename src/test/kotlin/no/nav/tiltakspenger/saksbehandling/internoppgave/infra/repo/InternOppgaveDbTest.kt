package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveGrunnlag
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.Test

/** Pinner lagringsformatet med ren mapping, mens radmapping og persistering testes gjennom servicen mot Postgres. */
class InternOppgaveDbTest {
    @Test
    fun `pinner grunnlag og oppgavetype på disk`() {
        val grunnlag = InternOppgaveGrunnlag.EndretTiltaksdeltakelse(
            tiltaksdeltakerId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01HSTRQBRM443VGB4WA822TE02"),
            hendelseId = TiltaksdeltakerHendelseId.fromString("tiltaksdeltakerhendelse_01HSTRQBRM443VGB4WA822TE01"),
            beskrivelse = "Ny \"sluttdato\"\nMå vurderes",
        )
        val json = """{"hendelseId":"tiltaksdeltakerhendelse_01HSTRQBRM443VGB4WA822TE01","beskrivelse":"Ny \"sluttdato\"\nMå vurderes"}"""
        grunnlag.toDbJson() shouldBe json
        InternOppgavetype.entries.associateWith { it.toDb().name } shouldBe mapOf(
            InternOppgavetype.ENDRET_TILTAKSDELTAKELSE to "ENDRET_TILTAKSDELTAKELSE",
        )
        InternOppgavetypeDb.valueOf("ENDRET_TILTAKSDELTAKELSE")
            .tilGrunnlag(grunnlag.nøkkel, json) shouldBe grunnlag
    }

    @Test
    fun `pinner alle løsninger på disk og leser dem tilbake`() {
        val behandlingId = RammebehandlingId.random()
        val løsninger = mapOf(
            InternOppgaveløsning.Forkastet to "FORKASTET",
            InternOppgaveløsning.Revurdering(InternOppgaveløsning.Revurderingstype.STANS, behandlingId) to "STANS",
            InternOppgaveløsning.Revurdering(InternOppgaveløsning.Revurderingstype.FORLENGELSE, behandlingId) to "FORLENGELSE",
            InternOppgaveløsning.Revurdering(InternOppgaveløsning.Revurderingstype.OMGJØRING, behandlingId) to "OMGJORING",
        )
        løsninger.keys.associateWith { it.toDb().name } shouldBe løsninger
        InternOppgaveløsningDb.entries.map { it.name }.toSet() shouldBe løsninger.values.toSet()
        løsninger.forEach { (løsning, lagret) ->
            val referanse = when (løsning) {
                InternOppgaveløsning.Forkastet -> null
                is InternOppgaveløsning.Revurdering -> løsning.behandlingId.toString()
            }
            InternOppgaveløsningDb.valueOf(lagret).tilDomene(referanse) shouldBe løsning
        }
    }
}
