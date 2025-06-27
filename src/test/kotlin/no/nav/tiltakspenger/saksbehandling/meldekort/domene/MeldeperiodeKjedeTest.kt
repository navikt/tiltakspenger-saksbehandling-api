package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test

class MeldeperiodeKjedeTest {

    @Test
    fun `legger ikke til ny periode dersom meldeperiodene er lik`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
        val m1 = ObjectMother.meldeperiode(sakId = sakId, saksnummer = saksnummer, fnr = fnr)
        val kjede = MeldeperiodeKjede(nonEmptyListOf(m1))
        val m2 =
            ObjectMother.meldeperiode(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                versjon = m1.versjon.inc(),
            )

        val (nyKjede) = kjede.leggTilMeldeperiode(m2)
        nyKjede.let {
            it.size shouldBe 1
            it.first() shouldBe m1
        }
    }

    @Test
    fun `kan legge til ny meldeperiode til kjeden`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
        val m1 = ObjectMother.meldeperiode(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
        )
        val kjede = MeldeperiodeKjede(nonEmptyListOf(m1))
        val m2 =
            ObjectMother.meldeperiode(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                versjon = m1.versjon.inc(),
                antallDagerForPeriode = 12,
                opprettet = m1.opprettet.plusDays(1),
            )

        val (nyKjede) = kjede.leggTilMeldeperiode(m2)
        nyKjede.let {
            it.size shouldBe 2
            it.first() shouldBe m1
            it.last() shouldBe m2
        }
    }
}
