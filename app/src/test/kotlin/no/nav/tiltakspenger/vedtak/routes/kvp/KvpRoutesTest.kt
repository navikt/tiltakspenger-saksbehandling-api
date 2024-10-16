package no.nav.tiltakspenger.vedtak.routes.kvp

import com.fasterxml.jackson.databind.ObjectMapper
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.libs.common.Rolle
import no.nav.tiltakspenger.libs.common.Roller
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUavklart
import no.nav.tiltakspenger.vedtak.clients.defaultObjectMapper
import no.nav.tiltakspenger.vedtak.routes.behandling.BEHANDLING_PATH
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.KVPVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.kvpRoutes
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider
import org.junit.jupiter.api.Test

class KvpRoutesTest {
    private val mockInnloggetSaksbehandlerProvider = mockk<InnloggetSaksbehandlerProvider>()

    private val objectMapper: ObjectMapper = defaultObjectMapper()
    private val periodeBrukerHarKvpEtterEndring = PeriodeDTO(fraOgMed = "2023-01-01", tilOgMed = "2023-01-03")
    private val saksbehandler =
        Saksbehandler(
            "Q123456",
            "Superman",
            "a@b.c",
            Roller(listOf(Rolle.SAKSBEHANDLER, Rolle.SKJERMING, Rolle.STRENGT_FORTROLIG_ADRESSE)),
        )

    @Test
    fun `test at endepunkt for henting og lagring av kvp fungerer`() = runTest {
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns saksbehandler

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
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            kvpVilkårService = tac.førstegangsbehandlingContext.kvpVilkårService,
                            behandlingService = tac.førstegangsbehandlingContext.behandlingService,
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
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns saksbehandler

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
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            kvpVilkårService = tac.førstegangsbehandlingContext.kvpVilkårService,
                            behandlingService = tac.førstegangsbehandlingContext.behandlingService,
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
              ],
              "årsakTilEndring": "FEIL_I_INNHENTET_DATA"
            }
        """.trimIndent()
    }
}
