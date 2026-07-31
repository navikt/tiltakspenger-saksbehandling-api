package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.infra.route.FnrDTO
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class HentEllerOpprettSakRouteTest {
    private val ident = Fnr.random()
    private val systembruker = ObjectMother.systembrukerHentEllerOpprettSak()

    @Test
    fun `hentEllerOpprettSak - sak finnes ikke - oppretter sak`() {
        withTestApplicationContext { tac ->
            val jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("hent_eller_opprett_sak"),
            )
            tac.leggTilBruker(jwt, systembruker)
            defaultRequestWithAssertions(
                HttpMethod.POST,
                SAKSNUMMER_PATH,
                jwt = jwt,
                forventet = ForventetRespons(status = 200),
                body = objectMapper.writeValueAsString(FnrDTO(ident.verdi)),
            )
            tac.sakContext.sakRepo.hentForFnr(ident) shouldNotBe null
        }
    }

    @Test
    fun `hentEllerOpprettSak - sak finnes - returnerer eksisterende sak`() {
        withTestApplicationContext { tac ->
            val sak = ObjectMother.nySak(fnr = ident)
            tac.sakContext.sakRepo.opprettSak(sak)
            val jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("hent_eller_opprett_sak"),
            )
            tac.leggTilBruker(jwt, systembruker)
            defaultRequestWithAssertions(
                HttpMethod.POST,
                SAKSNUMMER_PATH,
                jwt = jwt,
                forventet = ForventetRespons(status = 200),
                body = objectMapper.writeValueAsString(FnrDTO(ident.verdi)),
            ).apply {
                val response = objectMapper.readValue<SaksnummerResponse>(body)
                response.saksnummer shouldBe sak.saksnummer.verdi
            }
        }
    }

    @Test
    fun `hentEllerOpprettSak - feil rolle - returnerer 403`() {
        withTestApplicationContext { tac ->
            val sak = ObjectMother.nySak(fnr = ident)
            tac.sakContext.sakRepo.opprettSak(sak)
            val jwt = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("lagre_meldekort"),
            )
            tac.leggTilBruker(jwt, ObjectMother.systembrukerLagreMeldekort())
            defaultRequestWithAssertions(
                HttpMethod.POST,
                SAKSNUMMER_PATH,
                jwt = jwt,
                forventet = ForventetRespons(status = 403),
                body = objectMapper.writeValueAsString(FnrDTO(ident.verdi)),
            )
        }
    }
}
