package no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.aktor
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo.hentIdenthendelserForGammeltFnr
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseDto
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.hentStønadsstatistikkBrukerIder
import org.junit.jupiter.api.Test

/**
 * Tilstanden bygges gjennom prodstiene: behandlingen iverksettes via routene, slik at statistikken skrives av prodflyten, og identhendelsen kommer inn via [no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.AktorV2Consumer].
 * Jobben kjøres per id slik sveipet gjør i prod.
 * Sveipemetoden kalles ikke: den ville plukket opp parallelle testers hendelser, jf. «Fakes er per test, jobber sveiper over hele skjemaet» i `AGENTS-backend.md`.
 */
class IdenthendelseJobbTest {

    @Test
    fun `behandleIdenthendelse - hendelsen er ikke behandlet - produserer til kafka og oppdaterer i database`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val (sak) = iverksettSøknadsbehandling(tac = tac, fnr = gammeltFnr)
            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr)))
            val identhendelse = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()

            tac.identhendelseJobb.behandleIdenthendelse(identhendelse.id)

            tac.identhendelseProducer.produserteHendelser shouldBe listOf(
                identhendelse.id to IdenthendelseDto(gammeltFnr = gammeltFnr.verdi, nyttFnr = nyttFnr.verdi),
            )
            val behandlet = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()
            behandlet.produsertHendelse shouldNotBe null
            behandlet.oppdatertDatabase shouldNotBe null

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            oppdatertSak.fnr shouldBe nyttFnr
            oppdatertSak.søknader.single().fnr shouldBe nyttFnr

            val saksstatistikk = tac.sessionFactory.hentSaksstatistikk(sak.id)
            saksstatistikk.shouldNotBeEmpty()
            saksstatistikk.map { it.fnr }.distinct() shouldBe listOf(nyttFnr.verdi)
            val stønadsstatistikkBrukerIder = tac.sessionFactory.hentStønadsstatistikkBrukerIder(sak.id)
            stønadsstatistikkBrukerIder.shouldNotBeEmpty()
            stønadsstatistikkBrukerIder.distinct() shouldBe listOf(nyttFnr.verdi)
        }
    }

    @Test
    fun `behandleIdenthendelse - produsert på kafka, ikke oppdatert i db - oppdaterer database uten å produsere på nytt`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val (sak) = opprettSakOgSøknad(tac = tac, fnr = gammeltFnr)
            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr)))
            val identhendelse = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()
            // Et krasj mellom stegene i prod etterlater hendelsen produsert, men ikke databaseoppdatert.
            // Tilstanden konstrueres med prod-kallet jobben selv bruker for steg én.
            tac.identhendelseRepository.oppdaterProdusertHendelse(identhendelse.id)

            tac.identhendelseJobb.behandleIdenthendelse(identhendelse.id)

            tac.identhendelseProducer.produserteHendelser shouldBe emptyList()
            val behandlet = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()
            behandlet.oppdatertDatabase shouldNotBe null
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            oppdatertSak.fnr shouldBe nyttFnr
            oppdatertSak.søknader.single().fnr shouldBe nyttFnr
        }
    }

    @Test
    fun `behandleIdenthendelse - hendelsen er ferdig behandlet - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            opprettSakOgSøknad(tac = tac, fnr = gammeltFnr)
            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = nyttFnr, historiskeFnr = listOf(gammeltFnr)))
            val identhendelse = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()
            tac.identhendelseJobb.behandleIdenthendelse(identhendelse.id)
            val etterFørsteKjøring = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()

            tac.identhendelseJobb.behandleIdenthendelse(identhendelse.id)

            tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single() shouldBe etterFørsteKjøring
            tac.identhendelseProducer.produserteHendelser.size shouldBe 1
        }
    }

    @Test
    fun `hentIderSomIkkeErBehandlet - plukker kun opp hendelser som ikke er ferdig behandlet`() {
        withTestApplicationContextAndPostgres { tac ->
            val gammeltFnr = Fnr.random()
            val gammeltFnr2 = Fnr.random()
            opprettSakOgSøknad(tac = tac, fnr = gammeltFnr)
            opprettSakOgSøknad(tac = tac, fnr = gammeltFnr2)
            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = Fnr.random(), historiskeFnr = listOf(gammeltFnr)))
            tac.aktorV2Consumer.consume("key", aktor(gjeldendeFnr = Fnr.random(), historiskeFnr = listOf(gammeltFnr2)))
            val ubehandlet = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr).single()
            val ferdigBehandlet = tac.sessionFactory.hentIdenthendelserForGammeltFnr(gammeltFnr2).single()
            tac.identhendelseJobb.behandleIdenthendelse(ferdigBehandlet.id)

            val iderSomIkkeErBehandlet = tac.identhendelseRepository.hentIderSomIkkeErBehandlet()

            iderSomIkkeErBehandlet shouldContain ubehandlet.id
            iderSomIkkeErBehandlet shouldNotContain ferdigBehandlet.id
        }
    }
}
