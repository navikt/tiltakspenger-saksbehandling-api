package no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Dialoginnlegg
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveGrunnlag
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgaveløsning
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.InternOppgavetype
import no.nav.tiltakspenger.saksbehandling.internoppgave.domene.Løsningsbegrunnelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
    fun `pinner alle utfall på disk og leser dem tilbake`() {
        val behandlingId = RammebehandlingId.random()
        val lagredeUtfall = mapOf(
            InternOppgaveløsning.Utfall.Forkastet to "FORKASTET",
            InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.STANS, behandlingId) to "STANS",
            InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.FORLENGELSE, behandlingId) to "FORLENGELSE",
            InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.OMGJØRING, behandlingId) to "OMGJORING",
        )
        lagredeUtfall.keys.associateWith { it.toDb().name } shouldBe lagredeUtfall
        InternOppgaveløsningDb.entries.map { it.name }.toSet() shouldBe lagredeUtfall.values.toSet()
        lagredeUtfall.forEach { (utfall, lagret) ->
            val referanse = when (utfall) {
                InternOppgaveløsning.Utfall.Forkastet -> null
                is InternOppgaveløsning.Utfall.Revurdering -> utfall.behandlingId.toString()
            }
            InternOppgaveløsningDb.valueOf(lagret).tilUtfall(referanse) shouldBe utfall
        }
    }

    @Test
    fun `pinner begrunnelser på disk og leser dem tilbake`() {
        Løsningsbegrunnelse.Årsak.entries.associateWith { it.toDb().name } shouldBe mapOf(
            Løsningsbegrunnelse.Årsak.ENDRINGEN_ER_ALLEREDE_HÅNDTERT to "ENDRINGEN_ER_ALLEREDE_HANDTERT",
            Løsningsbegrunnelse.Årsak.ENDRINGEN_PÅVIRKER_IKKE_RETTEN to "ENDRINGEN_PAVIRKER_IKKE_RETTEN",
        )
        LøsningsbegrunnelseDb.entries.forEach {
            tilLøsningsbegrunnelse(it.name, null) shouldBe Løsningsbegrunnelse.Forhåndsdefinert(it.tilDomene())
            it.tilDomene().toDb() shouldBe it
        }
        tilLøsningsbegrunnelse(null, "Vurdert") shouldBe Løsningsbegrunnelse.Fritekst(NonBlankString.create("Vurdert"))
        shouldThrow<IllegalArgumentException> { tilLøsningsbegrunnelse(null, null) }
    }

    @Test
    fun `pinner dialogen på disk og leser den tilbake`() {
        val dialog = listOf(
            Dialoginnlegg("Z123456", LocalDateTime.parse("2026-09-22T10:15:30.123456"), NonBlankString.create("Første \"innlegg\"\nMed linjeskift")),
            Dialoginnlegg("Z654321", LocalDateTime.parse("2026-09-22T11:00"), NonBlankString.create("Andre")),
        )
        val json = """[{"saksbehandler":"Z123456","tidspunkt":"2026-09-22T10:15:30.123456","tekst":"Første \"innlegg\"\nMed linjeskift"},""" +
            """{"saksbehandler":"Z654321","tidspunkt":"2026-09-22T11:00:00","tekst":"Andre"}]"""
        dialog.toDbJson() shouldBe json
        json.tilDialog() shouldBe dialog
        emptyList<Dialoginnlegg>().toDbJson() shouldBe "[]"
        "[]".tilDialog() shouldBe emptyList()
    }
}
