package no.nav.tiltakspenger.vedtak.routes.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandlerMedAdmin
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import no.nav.tiltakspenger.vedtak.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider
import org.junit.jupiter.api.Test

class BehandlingBeslutterRoutesTest {
    private val innloggetSaksbehandlerProviderMock = mockk<InnloggetSaksbehandlerProvider>()
    private val behandlingService = mockk<BehandlingServiceImpl>()

    @Test
    fun `sjekk at man ikke kan sende inn uten beslutter rolle`() {
        every { innloggetSaksbehandlerProviderMock.hentInnloggetSaksbehandler(any()) } returns saksbehandler()

        val behandlingId = BehandlingId.random()
        testApplication {
            application {
                // vedtakTestApi()
                jacksonSerialization()
                routing {
                    behandlingBeslutterRoutes(
                        innloggetSaksbehandlerProviderMock,
                        behandlingService,
                    )
                }
            }
            shouldThrow<IllegalStateException> {
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/sendtilbake/$behandlingId")
                    },
                ) {
                    setBody(begrunnelseJson)
                }
            }
        }
    }

    @Test
    fun `sjekk at begrunnelse kan sendes inn`() {
        val begrunnelse = slot<String>()
        val bId = slot<BehandlingId>()
        val saksbehandler = slot<String>()
        val erAdmin = slot<Boolean>()

        every { innloggetSaksbehandlerProviderMock.hentInnloggetSaksbehandler(any()) } returns beslutter()
        every {
            behandlingService.sendTilbakeTilSaksbehandler(
                capture(bId),
                capture(saksbehandler),
                capture(begrunnelse),
                capture(erAdmin),
            )
        } returns Unit

        val behandlingId = BehandlingId.random()
        testApplication {
            application {
                // vedtakTestApi()
                jacksonSerialization()
                routing {
                    behandlingBeslutterRoutes(
                        innloggetSaksbehandlerProviderMock,
                        behandlingService,
                    )
                }
            }
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$behandlingPath/sendtilbake/$behandlingId")
                },
            ) {
                setBody(begrunnelseJson)
            }
                .apply {
                    status shouldBe HttpStatusCode.OK
                }
        }
        bId.captured shouldBe behandlingId
        saksbehandler.captured shouldBe "Z12345"
        begrunnelse.captured shouldBe "begrunnelse"
        erAdmin.captured shouldBe false
    }

    @Test
    fun `sjekk at man kan sende tilbake med admin rolle`() {
        val begrunnelse = slot<String>()
        val bId = slot<BehandlingId>()
        val saksbehandler = slot<String>()
        val erAdmin = slot<Boolean>()

        every { innloggetSaksbehandlerProviderMock.hentInnloggetSaksbehandler(any()) } returns saksbehandlerMedAdmin()
        every {
            behandlingService.sendTilbakeTilSaksbehandler(
                capture(bId),
                capture(saksbehandler),
                capture(begrunnelse),
                capture(erAdmin),
            )
        } returns Unit

        val behandlingId = BehandlingId.random()
        testApplication {
            application {
                // vedtakTestApi()
                jacksonSerialization()
                routing {
                    behandlingBeslutterRoutes(
                        innloggetSaksbehandlerProviderMock,
                        behandlingService,
                    )
                }
            }
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$behandlingPath/sendtilbake/$behandlingId")
                },
            ) {
                setBody(begrunnelseJson)
            }
                .apply {
                    status shouldBe HttpStatusCode.OK
                }
        }
        bId.captured shouldBe behandlingId
        saksbehandler.captured shouldBe "Z12345"
        begrunnelse.captured shouldBe "begrunnelse"
        erAdmin.captured shouldBe true
    }

    private val begrunnelseJson = """
        {
            "begrunnelse": "begrunnelse"
        }
    """.trimIndent()
}
