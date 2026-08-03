package no.nav.tiltakspenger.saksbehandling.person.identhendelser

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo.hentIdenthendelserForGammeltFnr
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * Tilstanden bygges gjennom prodstiene: saken opprettes via søknadsruta, og hendelsen kommer inn via [no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.AktorV2Consumer] slik den gjør i nais.
 */
class IdenthendelseServiceTest {

    @Test
    fun `behandleIdenthendelse - finnes ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()

            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = Fnr.random(), historiskeFnr = listOf(gammeltFnr)))

            tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr) shouldBe emptyList()
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes en sak - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = gammeltFnr)

            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr)))

            val identhendelseDb = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()
            identhendelseDb.gammeltFnr shouldBe gammeltFnr
            identhendelseDb.nyttFnr shouldBe nyttFnr
            identhendelseDb.sakId shouldBe sak.id
            identhendelseDb.produsertHendelse shouldBe null
            identhendelseDb.oppdatertDatabase shouldBe null
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes sak på nytt fnr - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            opprettSakOgSøknad(tac = tac, fnr = nyttFnr)

            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr)))

            tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr) shouldBe emptyList()
            tac.sessionFactory.hentIdenthendelserForGammeltFnr(nyttFnr) shouldBe emptyList()
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes sak på nytt og gammelt fnr - feiler`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            opprettSakOgSøknad(tac = tac, fnr = gammeltFnr)
            opprettSakOgSøknad(tac = tac, fnr = nyttFnr)

            assertFailsWith<IllegalStateException> {
                tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr)))
            }
        }
    }

    @Test
    fun `behandleIdenthendelse - finnes sak på to gamle fnr - feiler`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val gammeltFnr2 = Fnr.random()
            val nyttFnr = Fnr.random()
            opprettSakOgSøknad(tac = tac, fnr = gammeltFnr)
            opprettSakOgSøknad(tac = tac, fnr = gammeltFnr2)

            assertFailsWith<IllegalStateException> {
                tac.aktorV2Consumer.consume(
                    "key",
                    aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr, gammeltFnr2)),
                )
            }

            tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr) shouldBe emptyList()
            tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr2) shouldBe emptyList()
        }
    }

    @Test
    fun `behandleIdenthendelse - ingen gjeldende ident - ignoreres`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr1 = Fnr.random()
            val fnr2 = Fnr.random()
            opprettSakOgSøknad(tac = tac, fnr = fnr1)

            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = null, historiskeFnr = listOf(fnr1, fnr2)))

            tac.sessionFactory.hentIdenthendelserForGammeltFnr(fnr1) shouldBe emptyList()
            tac.sessionFactory.hentIdenthendelserForGammeltFnr(fnr2) shouldBe emptyList()
        }
    }
}
