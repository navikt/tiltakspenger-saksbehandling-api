package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import org.junit.jupiter.api.Test

class MeldeperiodeTest {

    @Test
    fun `2 meldeperidoer er lik`() {
        val sakId = SakId.random()
        val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
        val fnr = Fnr.random()
        val m1 = ObjectMother.meldeperiode(sakId = sakId, saksnummer = saksnummer, fnr = fnr)
        val m2 = ObjectMother.meldeperiode(sakId = sakId, saksnummer = saksnummer, fnr = fnr)
        m1.erLik(m2) shouldBe true
    }

    @Test
    fun `2 meldeperidoer er ikke lik`() {
        val m1 = ObjectMother.meldeperiode()
        val m2 = ObjectMother.meldeperiode()
        m1.erLik(m2) shouldBe false
    }
}
