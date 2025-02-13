package no.nav.tiltakspenger.vedtak.routes.kravfrist

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.januarDateTime
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUavklart
import no.nav.tiltakspenger.objectmothers.nySøknad
import no.nav.tiltakspenger.vedtak.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.behandlingRoutes
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist.KravfristVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist.kravfristRoutes
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import org.junit.jupiter.api.Test

internal class KravfristRoutesTest {

    @Test
    fun `test at endepunkt for henting av kravfrist fungerer og blir OPPFYLT`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart()
            val behandlingId = sak.førstegangsbehandling!!.id

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        kravfristRoutes(
                            tokenService = tac.tokenService,
                            behandlingService = tac.behandlingContext.behandlingService,
                            auditService = tac.personContext.auditService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/kravfrist")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val kravfristVilkår = objectMapper.readValue<KravfristVilkårDTO>(bodyAsText())
                    kravfristVilkår.samletUtfall shouldBe SamletUtfallDTO.OPPFYLT
                }
            }
        }
    }

    @Test
    fun `test at behandlingen ikke kan opprettes om om det er søkt for lenge etter fristen`() = runTest {
        with(TestApplicationContext()) {
            val tac = this

            val vurderingsperiode = Periode(1.januar(2021), 31.januar(2021))
            val sak = ObjectMother.nySak()
            tac.sakContext.sakRepo.opprettSak(sak)
            val søknad = this.nySøknad(
                fnr = sak.fnr,
                periode = vurderingsperiode,
                tidsstempelHosOss = 1.januarDateTime(2022),
                sak = sak,
            )

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        behandlingRoutes(
                            tiltaksdeltagelseVilkårService = tac.behandlingContext.tiltaksdeltagelseVilkårService,
                            kvpVilkårService = tac.behandlingContext.kvpVilkårService,
                            livsoppholdVilkårService = tac.behandlingContext.livsoppholdVilkårService,
                            tokenService = tac.tokenService,
                            behandlingService = tac.behandlingContext.behandlingService,
                            sakService = tac.sakContext.sakService,
                            auditService = tac.personContext.auditService,
                            søknadService = tac.søknadContext.søknadService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/startbehandling")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ) {
                    setBody("""{"id":"${søknad.id}"}""")
                }.apply {
                    withClue(
                        "Response details:\n" +
                            "Status: ${this.status}\n" +
                            "Content-Type: ${this.contentType()}\n" +
                            "Body: ${this.bodyAsText()}\n",
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                        bodyAsText() shouldBe """
                            {"melding":"Vi støtter ikke delvis innvilgelse eller avslag.","kode":"støtter_ikke_delvis_innvilgelse_eller_avslag"}
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
