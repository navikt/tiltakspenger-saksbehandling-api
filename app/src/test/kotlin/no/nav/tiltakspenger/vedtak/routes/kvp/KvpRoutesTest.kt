package no.nav.tiltakspenger.vedtak.routes.kvp

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUavklart
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.KVPVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.kvpRoutes
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import org.junit.jupiter.api.Test

class KvpRoutesTest {

    private val periodeBrukerHarKvpEtterEndring = PeriodeDTO(fraOgMed = "2023-01-01", tilOgMed = "2023-01-03")
    private val saksbehandler = ObjectMother.saksbehandler(
        "Q123456",
        "Superman",
        "a@b.c",
        Saksbehandlerroller(
            listOf(
                Saksbehandlerrolle.SAKSBEHANDLER,
                Saksbehandlerrolle.SKJERMING,
                Saksbehandlerrolle.STRENGT_FORTROLIG_ADRESSE,
            ),
        ),
    )

    @Test
    fun `test at endepunkt for henting og lagring av kvp fungerer`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart(
                saksbehandler = saksbehandler,
            )
            val behandlingId = sak.førstegangsbehandling.id

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        kvpRoutes(
                            tokenService = tac.tokenService,
                            kvpVilkårService = tac.behandlingContext.kvpVilkårService,
                            behandlingService = tac.behandlingContext.behandlingService,
                            auditService = tac.personContext.auditService,
                        )
                    }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/kvp")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val kvpVilkår = objectMapper.readValue<KVPVilkårDTO>(bodyAsText())
                    kvpVilkår.avklartSaksopplysning.periodeMedDeltagelse.periode shouldNotBe periodeBrukerHarKvpEtterEndring
                }
            }
        }
    }

    @Test
    fun `test at samlet utfall for kvp blir OPPFYLT om bruker ikke går på kvp i vurderingsperioden`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            val sak = this.førstegangsbehandlingUavklart(
                saksbehandler = saksbehandler,
            )
            val behandlingId = sak.førstegangsbehandling.id

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        kvpRoutes(
                            tokenService = tac.tokenService,
                            kvpVilkårService = tac.behandlingContext.kvpVilkårService,
                            behandlingService = tac.behandlingContext.behandlingService,
                            auditService = tac.personContext.auditService,
                        )
                    }
                }

                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$BEHANDLING_PATH/$behandlingId/vilkar/kvp")
                    },
                    jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val kvpVilkår = objectMapper.readValue<KVPVilkårDTO>(bodyAsText())
                    kvpVilkår.samletUtfall shouldBe SamletUtfallDTO.OPPFYLT
                }
            }
        }
    }

    private fun bodyEndreKvp(
        periodeDTO: PeriodeDTO,
        deltar: Boolean,
    ): String {
        val deltarString = if (deltar) "true" else "false"
        return """
            {
              "ytelseForPeriode": [
                {
                  "periode": {
                    "fraOgMed": "${periodeDTO.fraOgMed}",
                    "tilOgMed": "${periodeDTO.tilOgMed}"
                  },
                  "deltar": $deltarString
                }
              ]
            }
        """.trimIndent()
    }
}
