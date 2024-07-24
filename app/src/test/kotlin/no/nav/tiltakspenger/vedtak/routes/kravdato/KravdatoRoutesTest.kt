package no.nav.tiltakspenger.vedtak.routes.kravdato

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
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
import no.nav.tiltakspenger.felles.Rolle
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.personopplysningFødselsdato
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Førstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.utbetaling.UtbetalingServiceImpl
import no.nav.tiltakspenger.vedtak.clients.brevpublisher.BrevPublisherGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.defaultObjectMapper
import no.nav.tiltakspenger.vedtak.clients.meldekort.MeldekortGrunnlagGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.tiltak.TiltakGatewayImpl
import no.nav.tiltakspenger.vedtak.db.PostgresTestcontainer
import no.nav.tiltakspenger.vedtak.db.TestDataHelper
import no.nav.tiltakspenger.vedtak.db.flywayMigrate
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
import no.nav.tiltakspenger.vedtak.routes.behandling.behandlingPath
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravdato.KravdatoVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravdato.kravdatoRoutes
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime

@Testcontainers
class KravdatoRoutesTest {

    companion object {
        @Container
        val postgresContainer = PostgresTestcontainer
    }

    @BeforeEach
    fun setup() {
        flywayMigrate()
    }

    private val mockInnloggetSaksbehandlerProvider = mockk<InnloggetSaksbehandlerProvider>()
    private val mockedUtbetalingServiceImpl = mockk<UtbetalingServiceImpl>()
    private val mockBrevPublisherGateway = mockk<BrevPublisherGatewayImpl>()
    private val mockMeldekortGrunnlagGateway = mockk<MeldekortGrunnlagGatewayImpl>()
    private val mockTiltakGateway = mockk<TiltakGatewayImpl>()

    private val objectMapper: ObjectMapper = defaultObjectMapper()
    private val mockSaksbehandler = Saksbehandler(
        "Q123456",
        "Superman",
        "a@b.c",
        listOf(Rolle.SAKSBEHANDLER, Rolle.SKJERMING, Rolle.STRENGT_FORTROLIG_ADRESSE),
    )

    @Test
    fun `test at endepunkt for henting av kravdato fungerer og blir OPPFYLT`() {
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns mockSaksbehandler

        val sakId = SakId.random()
        val søknad = nySøknad(
            periode = Periode(fraOgMed = LocalDate.now().minusMonths(2), tilOgMed = LocalDate.now().minusMonths(1)),
            tidsstempelHosOss = LocalDateTime.now(),
        )

        val objectMotherSak = ObjectMother.sakMedOpprettetBehandling(
            id = sakId,
            behandlinger = listOf(
                Førstegangsbehandling.opprettBehandling(
                    sakId,
                    søknad,
                    personopplysningFødselsdato(),
                ),
            ),
        )

        val behandlingId = objectMotherSak.behandlinger.first().id.toString()

        withMigratedDb { dataSource ->
            val testDataHelper = TestDataHelper(dataSource)

            testDataHelper.sessionFactory.withTransaction {
                testDataHelper.sakRepo.lagre(objectMotherSak)
            }

            val behandlingService = BehandlingServiceImpl(
                behandlingRepo = testDataHelper.behandlingRepo,
                vedtakRepo = testDataHelper.vedtakRepo,
                personopplysningRepo = testDataHelper.personopplysningerRepo,
                utbetalingService = mockedUtbetalingServiceImpl,
                brevPublisherGateway = mockBrevPublisherGateway,
                meldekortGrunnlagGateway = mockMeldekortGrunnlagGateway,
                tiltakGateway = mockTiltakGateway,
                sakRepo = testDataHelper.sakRepo,
                attesteringRepo = testDataHelper.attesteringRepo,
                sessionFactory = testDataHelper.sessionFactory,
            )

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        kravdatoRoutes(
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            behandlingService = behandlingService,
                        )
                    }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/kravdato")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val kravdatoVilkår = objectMapper.readValue<KravdatoVilkårDTO>(bodyAsText())
                    kravdatoVilkår.samletUtfall shouldBe SamletUtfallDTO.OPPFYLT
                }
            }
        }
    }

    @Test
    fun `test at kravdato gir IKKE_OPPFYLT om det er søkt for lenge etter fristen`() {
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns mockSaksbehandler

        val sakId = SakId.random()
        val søknad = nySøknad(
            periode = Periode(fraOgMed = LocalDate.now().minusMonths(2), tilOgMed = LocalDate.now().minusMonths(1)),
            tidsstempelHosOss = LocalDateTime.now().plusMonths(4),
        )

        val objectMotherSak = ObjectMother.sakMedOpprettetBehandling(
            id = sakId,
            behandlinger = listOf(
                Førstegangsbehandling.opprettBehandling(
                    sakId,
                    søknad,
                    personopplysningFødselsdato(),
                ),
            ),
        )
        val behandlingId = objectMotherSak.behandlinger.first().id.toString()
        withMigratedDb { dataSource ->
            val testDataHelper = TestDataHelper(dataSource)

            testDataHelper.sessionFactory.withTransaction {
                testDataHelper.sakRepo.lagre(objectMotherSak)
            }

            val behandlingService = BehandlingServiceImpl(
                behandlingRepo = testDataHelper.behandlingRepo,
                vedtakRepo = testDataHelper.vedtakRepo,
                personopplysningRepo = testDataHelper.personopplysningerRepo,
                utbetalingService = mockedUtbetalingServiceImpl,
                brevPublisherGateway = mockBrevPublisherGateway,
                meldekortGrunnlagGateway = mockMeldekortGrunnlagGateway,
                tiltakGateway = mockTiltakGateway,
                sakRepo = testDataHelper.sakRepo,
                attesteringRepo = testDataHelper.attesteringRepo,
                sessionFactory = testDataHelper.sessionFactory,
            )

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        kravdatoRoutes(
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            behandlingService = behandlingService,
                        )
                    }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/kravdato")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val kravdatoVilkår = objectMapper.readValue<KravdatoVilkårDTO>(bodyAsText())
                    kravdatoVilkår.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT
                }
            }
        }
    }
}
