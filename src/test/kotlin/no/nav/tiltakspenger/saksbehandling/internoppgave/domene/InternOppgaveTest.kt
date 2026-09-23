package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import arrow.core.left
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.annenTestsaksbehandler
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.testbegrunnelse
import no.nav.tiltakspenger.saksbehandling.internoppgave.infra.repo.testsaksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

class InternOppgaveTest {
    private val sakId = SakId.random()
    private val tiltaksdeltakerId = TiltaksdeltakerId.random()
    private val grunnlag = grunnlag()
    private val saksbehandler = testsaksbehandler
    private val annenSaksbehandler = annenTestsaksbehandler

    @Test
    fun `oppretter en åpen ufordelt oppgave med unik id og versjon null`() {
        val oppgave = opprett()

        oppgave.id shouldNotBe opprett().id
        oppgave.sakId shouldBe sakId
        oppgave.grunnlag shouldBe grunnlag
        oppgave.type shouldBe InternOppgavetype.ENDRET_TILTAKSDELTAKELSE
        oppgave.nøkkel shouldBe tiltaksdeltakerId.toString()
        oppgave.opprettet shouldBe nå(fixedClock)
        oppgave.sistEndret shouldBe nå(fixedClock)
        oppgave.versjon shouldBe 0L
        oppgave.saksbehandler shouldBe null
        oppgave.løsning shouldBe null
        oppgave.erLøst shouldBe false
        oppgave.dialog shouldBe emptyList()
    }

    @Test
    fun `tar en ufordelt oppgave og bevarer identitet og grunnlag`() {
        val oppgave = opprett()
        val tildelt = oppgave.ta(saksbehandler, clock(1)).getOrFail()

        tildelt.saksbehandler shouldBe saksbehandler.navIdent
        tildelt.versjon shouldBe 1L
        tildelt.sistEndret shouldBe nå(clock(1))
        tildelt.id shouldBe oppgave.id
        tildelt.sakId shouldBe oppgave.sakId
        tildelt.opprettet shouldBe oppgave.opprettet
        tildelt.grunnlag shouldBe oppgave.grunnlag
        tildelt.erLøst shouldBe false
        oppgave.saksbehandler shouldBe null
        oppgave.versjon shouldBe 0L
    }

    @Test
    fun `kan ikke ta en oppgave som allerede er tildelt heller ikke til seg selv`() {
        val tildelt = opprett().ta(saksbehandler, clock(1)).getOrFail()

        listOf(saksbehandler, annenSaksbehandler).forEach {
            tildelt.ta(it, clock(2)) shouldBe InternOppgaveFeil.AlleredeTildelt.left()
        }
        tildelt.versjon shouldBe 1L
        tildelt.sistEndret shouldBe nå(clock(1))
    }

    @Test
    fun `eier kan legge tilbake og en annen saksbehandler kan ta oppgaven`() {
        val oppgave = opprett()
        val tildelt = oppgave.ta(saksbehandler, clock(1)).getOrFail()
        val tilbake = tildelt.leggTilbake(saksbehandler, clock(2)).getOrFail()
        val overtatt = tilbake.ta(annenSaksbehandler, clock(3)).getOrFail()

        tilbake.saksbehandler shouldBe null
        tilbake.versjon shouldBe 2L
        tilbake.sistEndret shouldBe nå(clock(2))
        tilbake.erLøst shouldBe false
        tilbake.id shouldBe oppgave.id
        tilbake.sakId shouldBe sakId
        tilbake.grunnlag shouldBe grunnlag
        tilbake.opprettet shouldBe oppgave.opprettet
        overtatt.saksbehandler shouldBe annenSaksbehandler.navIdent
        overtatt.versjon shouldBe 3L
        overtatt.sistEndret shouldBe nå(clock(3))
        tildelt.saksbehandler shouldBe saksbehandler.navIdent
    }

    @Test
    fun `en annen saksbehandler kan overta en tildelt oppgave`() {
        val tildelt = opprett().ta(saksbehandler, clock(1)).getOrFail()
        val overtatt = tildelt.overta(annenSaksbehandler, clock(2)).getOrFail()

        overtatt.saksbehandler shouldBe annenSaksbehandler.navIdent
        overtatt.versjon shouldBe 2L
        overtatt.sistEndret shouldBe nå(clock(2))
        overtatt.id shouldBe tildelt.id
        overtatt.grunnlag shouldBe tildelt.grunnlag
        overtatt.opprettet shouldBe tildelt.opprettet
        tildelt.leggTilbake(saksbehandler, clock(3)).getOrFail()
        overtatt.leggTilbake(saksbehandler, clock(3)) shouldBe InternOppgaveFeil.IkkeEier.left()
        overtatt.løs(annenSaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, clock(3)).getOrFail()
    }

    @Test
    fun `kan ikke overta en ufordelt oppgave eller fra seg selv`() {
        val ufordelt = opprett()
        val tildelt = ufordelt.ta(saksbehandler, clock(1)).getOrFail()

        ufordelt.overta(saksbehandler, clock(2)) shouldBe InternOppgaveFeil.IkkeTildelt.left()
        tildelt.overta(saksbehandler, clock(2)) shouldBe InternOppgaveFeil.KanIkkeOvertaFraSegSelv.left()
        tildelt.versjon shouldBe 1L
        tildelt.sistEndret shouldBe nå(clock(1))
    }

    @Test
    fun `kun eier kan legge tilbake eller løse en åpen oppgave`() {
        val ufordelt = opprett()
        val tildelt = ufordelt.ta(saksbehandler, clock(1)).getOrFail()

        listOf(ufordelt, tildelt).forEach {
            it.leggTilbake(annenSaksbehandler, clock(2)) shouldBe InternOppgaveFeil.IkkeEier.left()
            it.løs(annenSaksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, clock(2)) shouldBe
                InternOppgaveFeil.IkkeEier.left()
        }
        ufordelt.versjon shouldBe 0L
        tildelt.versjon shouldBe 1L
        tildelt.sistEndret shouldBe nå(clock(1))
    }

    @Test
    fun `kan forkaste eller registrere stans forlengelse og omgjøring med eksisterende behandlingsid`() {
        val behandlingId = RammebehandlingId.random()
        val utfall = listOf(
            InternOppgaveløsning.Utfall.Forkastet,
            InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.STANS, behandlingId),
            InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.FORLENGELSE, behandlingId),
            InternOppgaveløsning.Utfall.Revurdering(InternOppgaveløsning.Revurderingstype.OMGJØRING, behandlingId),
        )

        utfall.forEach { utfall ->
            val tildelt = opprett().ta(saksbehandler, clock(1)).getOrFail()
            val løst = tildelt.løs(saksbehandler, utfall, testbegrunnelse, clock(2)).getOrFail()

            løst.løsning shouldBe InternOppgaveløsning(utfall, testbegrunnelse, nå(clock(2)))
            løst.erLøst shouldBe true
            løst.sistEndret shouldBe nå(clock(2))
            løst.saksbehandler shouldBe saksbehandler.navIdent
            løst.versjon shouldBe 2L
            løst.id shouldBe tildelt.id
            løst.sakId shouldBe sakId
            løst.opprettet shouldBe tildelt.opprettet
            løst.grunnlag shouldBe grunnlag
            tildelt.løsning shouldBe null
        }
    }

    @Test
    fun `begrunnelsen kan være forhåndsdefinert eller fritekst`() {
        val begrunnelser = Løsningsbegrunnelse.Årsak.entries.map { Løsningsbegrunnelse.Forhåndsdefinert(it) } +
            Løsningsbegrunnelse.Fritekst(NonBlankString.create("Deltakeren har allerede fått stans"))

        begrunnelser.forEach { begrunnelse ->
            opprett().ta(saksbehandler, clock(1)).getOrFail()
                .løs(saksbehandler, InternOppgaveløsning.Utfall.Forkastet, begrunnelse, clock(2)).getOrFail()
                .løsning!!.begrunnelse shouldBe begrunnelse
        }
    }

    @Test
    fun `alle saksbehandlere kan skrive i dialogen på en åpen oppgave uten at versjonen endres`() {
        val ufordelt = opprett()
        val første = ufordelt.leggTilDialoginnlegg(annenSaksbehandler, tekst("Hvem tar denne?"), clock(1)).getOrFail()
        val tildelt = første.ta(saksbehandler, clock(2)).getOrFail()
        val andre = tildelt.leggTilDialoginnlegg(annenSaksbehandler, tekst("Sjekk sluttdatoen"), clock(3)).getOrFail()
        val tredje = andre.leggTilDialoginnlegg(saksbehandler, tekst("Takk"), clock(4)).getOrFail()

        tredje.dialog shouldBe listOf(
            Dialoginnlegg(annenSaksbehandler.navIdent, nå(clock(1)), tekst("Hvem tar denne?")),
            Dialoginnlegg(annenSaksbehandler.navIdent, nå(clock(3)), tekst("Sjekk sluttdatoen")),
            Dialoginnlegg(saksbehandler.navIdent, nå(clock(4)), tekst("Takk")),
        )
        første.versjon shouldBe ufordelt.versjon
        første.sistEndret shouldBe ufordelt.sistEndret
        tredje.versjon shouldBe tildelt.versjon
        tredje.sistEndret shouldBe tildelt.sistEndret
        tredje.saksbehandler shouldBe saksbehandler.navIdent
        ufordelt.dialog shouldBe emptyList()
    }

    @Test
    fun `dialogen følger med gjennom andre operasjoner og kan ikke utvides etter løsning`() {
        val medDialog = opprett().leggTilDialoginnlegg(annenSaksbehandler, tekst("Innspill"), clock(1)).getOrFail()
        val løst = medDialog.ta(saksbehandler, clock(2)).getOrFail()
            .oppdaterGrunnlag(grunnlag(), clock(3)).getOrFail()
            .leggTilbake(saksbehandler, clock(4)).getOrFail()
            .ta(saksbehandler, clock(5)).getOrFail()
            .løs(saksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, clock(6)).getOrFail()

        løst.dialog shouldBe medDialog.dialog
        listOf(saksbehandler, annenSaksbehandler).forEach {
            løst.leggTilDialoginnlegg(it, tekst("For sent"), clock(7)) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        }
    }

    @Test
    fun `dialoginnlegg krever saksbehandler og maskerer teksten`() {
        listOf("", " ", "\n\t").forEach {
            shouldThrow<IllegalArgumentException> { opprett().leggTilDialoginnlegg(ObjectMother.saksbehandler(navIdent = it), tekst("Hei"), clock(1)) }
        }
        val innlegg = Dialoginnlegg(saksbehandler.navIdent, nå(fixedClock), tekst("Personlig opplysning"))
        innlegg.toString() shouldNotContain "Personlig opplysning"
        Løsningsbegrunnelse.Fritekst(tekst("Personlig opplysning")).toString() shouldNotContain "Personlig opplysning"
    }

    @Test
    fun `løste oppgaver avviser alle operasjoner uavhengig av eier og nytt grunnlag`() {
        val løst = opprett()
            .ta(saksbehandler, clock(1)).getOrFail()
            .løs(saksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, clock(2)).getOrFail()

        listOf(saksbehandler, annenSaksbehandler).forEach {
            løst.ta(it, clock(3)) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
            løst.leggTilbake(it, clock(3)) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
            løst.overta(it, clock(3)) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
            løst.løs(it, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, clock(3)) shouldBe
                InternOppgaveFeil.OppgavenErLøst.left()
        }
        løst.oppdaterGrunnlag(grunnlag(), clock(3)) shouldBe InternOppgaveFeil.OppgavenErLøst.left()
        løst.oppdaterGrunnlag(grunnlag(TiltaksdeltakerId.random()), clock(3)) shouldBe
            InternOppgaveFeil.OppgavenErLøst.left()
        løst.versjon shouldBe 2L
        løst.sistEndret shouldBe nå(clock(2))
    }

    @Test
    fun `systemet kan oppdatere grunnlaget både før og etter tildeling uten å endre eier`() {
        val ufordelt = opprett()
        val tildelt = ufordelt.ta(saksbehandler, clock(1)).getOrFail()
        val nyttGrunnlag = grunnlag(beskrivelse = "En ny endring i tiltaksdeltakelsen")

        listOf(ufordelt, tildelt).forEach {
            val oppdatert = it.oppdaterGrunnlag(nyttGrunnlag, clock(2)).getOrFail()

            oppdatert.grunnlag shouldBe nyttGrunnlag
            oppdatert.saksbehandler shouldBe it.saksbehandler
            oppdatert.versjon shouldBe it.versjon + 1
            oppdatert.sistEndret shouldBe nå(clock(2))
            oppdatert.opprettet shouldBe it.opprettet
            oppdatert.id shouldBe it.id
            oppdatert.sakId shouldBe it.sakId
            oppdatert.type shouldBe it.type
            oppdatert.nøkkel shouldBe it.nøkkel
            oppdatert.erLøst shouldBe false
            it.grunnlag shouldBe grunnlag
        }
    }

    @Test
    fun `kan ikke bytte tiltaksdeltaker ved oppdatering av grunnlaget`() {
        val oppgave = opprett()

        oppgave.oppdaterGrunnlag(grunnlag(TiltaksdeltakerId.random()), clock(1)) shouldBe
            InternOppgaveFeil.AnnetGrunnlag.left()
        oppgave.grunnlag shouldBe grunnlag
        oppgave.versjon shouldBe 0L
    }

    @Test
    fun `versjonen avanserer ved hver operasjon selv når klokken står stille og grunnlaget er likt`() {
        val oppgave = opprett()
        val tildelt = oppgave.ta(saksbehandler, fixedClock).getOrFail()
        val oppdatert = tildelt.oppdaterGrunnlag(grunnlag, fixedClock).getOrFail()
        val tilbake = oppdatert.leggTilbake(saksbehandler, fixedClock).getOrFail()
        val tattPåNytt = tilbake.ta(saksbehandler, fixedClock).getOrFail()
        val løst = tattPåNytt.løs(saksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, fixedClock).getOrFail()

        listOf(oppgave, tildelt, oppdatert, tilbake, tattPåNytt, løst).forEachIndexed { indeks, verdi ->
            verdi.versjon shouldBe indeks.toLong()
            verdi.sistEndret shouldBe nå(fixedClock)
        }
        tildelt.versjon shouldNotBe oppdatert.versjon
        tildelt.versjon shouldNotBe tattPåNytt.versjon
    }

    @Test
    fun `tillater ikke at klokke går bakover ved endringer`() {
        val tildelt = opprett().ta(saksbehandler, clock(2)).getOrFail()

        shouldThrow<IllegalArgumentException> { opprett().ta(saksbehandler, clock(-1)) }
        shouldThrow<IllegalArgumentException> { tildelt.leggTilbake(saksbehandler, clock(1)) }
        shouldThrow<IllegalArgumentException> { tildelt.overta(annenSaksbehandler, clock(1)) }
        shouldThrow<IllegalArgumentException> {
            tildelt.løs(saksbehandler, InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, clock(1))
        }
        shouldThrow<IllegalArgumentException> { tildelt.oppdaterGrunnlag(grunnlag(), clock(1)) }
    }

    @Test
    fun `saksbehandler og beskrivelse kan ikke være blanke`() {
        listOf("", " ", "\n\t").forEach {
            shouldThrow<IllegalArgumentException> { opprett().ta(ObjectMother.saksbehandler(navIdent = it), fixedClock) }
            shouldThrow<IllegalArgumentException> { grunnlag(beskrivelse = it) }
            shouldThrow<IllegalArgumentException> { fraLagretTilstand(saksbehandler = it) }
        }
    }

    @Test
    fun `fritekst maskeres både i grunnlaget og i oppgaven`() {
        grunnlag.toString() shouldBe
            "EndretTiltaksdeltakelse(tiltaksdeltakerId=$tiltaksdeltakerId, hendelseId=${grunnlag.hendelseId}, beskrivelse=*****)"
        opprett().toString() shouldNotContain grunnlag.beskrivelse
    }

    @Test
    fun `avviser negativ versjon og endring før opprettelse`() {
        shouldThrow<IllegalArgumentException> { fraLagretTilstand(versjon = -1) }
        shouldThrow<IllegalArgumentException> { fraLagretTilstand(sistEndret = nå(clock(-1))) }
    }

    @Test
    fun `løst oppgave må ha eier og løst må være lik sist endret`() {
        shouldThrow<IllegalArgumentException> {
            fraLagretTilstand(løsning = InternOppgaveløsning(InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, nå(fixedClock)))
        }
        listOf(nå(clock(-1)), nå(clock(1))).forEach {
            shouldThrow<IllegalArgumentException> {
                fraLagretTilstand(
                    saksbehandler = saksbehandler.navIdent,
                    løsning = InternOppgaveløsning(InternOppgaveløsning.Utfall.Forkastet, testbegrunnelse, it),
                )
            }
        }
    }

    private fun opprett(): InternOppgave = InternOppgave.opprett(sakId, grunnlag, fixedClock)

    private fun grunnlag(
        tiltaksdeltakerId: TiltaksdeltakerId = this.tiltaksdeltakerId,
        beskrivelse: String = "Tiltaksdeltakelsens sluttdato er endret",
    ) = InternOppgaveGrunnlag.EndretTiltaksdeltakelse(
        tiltaksdeltakerId = tiltaksdeltakerId,
        hendelseId = TiltaksdeltakerHendelseId.random(),
        beskrivelse = beskrivelse,
    )

    private fun tekst(verdi: String) = NonBlankString.create(verdi)

    private fun clock(minutter: Long): Clock = Clock.offset(fixedClock, Duration.ofMinutes(minutter))

    private fun fraLagretTilstand(
        sistEndret: LocalDateTime = nå(fixedClock),
        versjon: Long = 0,
        saksbehandler: String? = null,
        løsning: InternOppgaveløsning? = null,
    ) = InternOppgave(
        id = InternOppgaveId.random(),
        sakId = sakId,
        grunnlag = grunnlag,
        opprettet = nå(fixedClock),
        sistEndret = sistEndret,
        versjon = versjon,
        saksbehandler = saksbehandler,
        løsning = løsning,
        dialog = emptyList(),
    )
}
