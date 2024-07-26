package no.nav.tiltakspenger.vedtak.routes.livsopphold

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
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
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.fraOgMedDatoJa
import no.nav.tiltakspenger.objectmothers.ObjectMother.ja
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.periodeJa
import no.nav.tiltakspenger.objectmothers.ObjectMother.personopplysningFødselsdato
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Førstegangsbehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.utbetaling.UtbetalingServiceImpl
import no.nav.tiltakspenger.vedtak.clients.brevpublisher.BrevPublisherGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.defaultObjectMapper
import no.nav.tiltakspenger.vedtak.clients.meldekort.MeldekortGrunnlagGatewayImpl
import no.nav.tiltakspenger.vedtak.clients.tiltak.TiltakGatewayImpl
import no.nav.tiltakspenger.vedtak.db.TestDataHelper
import no.nav.tiltakspenger.vedtak.db.withMigratedDb
import no.nav.tiltakspenger.vedtak.routes.behandling.behandlingPath
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold.LivsoppholdVilkårDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold.livsoppholdRoutes
import no.nav.tiltakspenger.vedtak.routes.configureExceptions
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import no.nav.tiltakspenger.vedtak.tilgang.InnloggetSaksbehandlerProvider
import org.junit.jupiter.api.Test

class LivsoppholdRoutesTest {

    private val mockInnloggetSaksbehandlerProvider = mockk<InnloggetSaksbehandlerProvider>()
    private val mockedUtbetalingServiceImpl = mockk<UtbetalingServiceImpl>()
    private val mockBrevPublisherGateway = mockk<BrevPublisherGatewayImpl>()
    private val mockMeldekortGrunnlagGateway = mockk<MeldekortGrunnlagGatewayImpl>()
    private val mockTiltakGateway = mockk<TiltakGatewayImpl>()

    private val objectMapper: ObjectMapper = defaultObjectMapper()

    private val saksbehandlerIdent = "Q123456"
    private val mockSaksbehandler = Saksbehandler(
        saksbehandlerIdent,
        "Superman",
        "a@b.c",
        listOf(Rolle.SAKSBEHANDLER, Rolle.SKJERMING, Rolle.STRENGT_FORTROLIG_ADRESSE),
    )

    @Test
    fun `test at endepunkt for henting og lagring av livsopphold fungerer`() {
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns mockSaksbehandler

        val objectMotherSak = ObjectMother.sakMedOpprettetBehandling(løpenummer = 1012)
        val behandlingId = objectMotherSak.behandlinger.first().id.toString()
        val vurderingsPeriode = objectMotherSak.behandlinger.first().vurderingsperiode

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
                sakRepo = testDataHelper.sakRepo,
                attesteringRepo = testDataHelper.attesteringRepo,
                sessionFactory = testDataHelper.sessionFactory,
            )
            val livsoppholdVilkårService = LivsoppholdVilkårServiceImpl(
                behandlingRepo = testDataHelper.behandlingRepo,
                behandlingService = behandlingService,
            )

            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        livsoppholdRoutes(
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            livsoppholdVilkårService = livsoppholdVilkårService,
                            behandlingService = behandlingService,
                        )
                    }
                }

                // Sjekk at man kan kjøre Get
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/livsopphold")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val livsoppholdVilkår = objectMapper.readValue<LivsoppholdVilkårDTO>(bodyAsText())
                    livsoppholdVilkår.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeFalse()
                    livsoppholdVilkår.avklartSaksopplysning.saksbehandler shouldBe null
                }

                // Sjekk at man kan si at bruker ikke har livsoppholdytelser
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/livsopphold")
                    },
                ) {
                    setBody(bodyLivsoppholdYtelse(vurderingsPeriode.toDTO(), false))
                }.apply {
                    status shouldBe HttpStatusCode.Created
                }

                // Sjekk at dataene har blitt oppdatert
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/livsopphold")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    val livsoppholdVilkår = objectMapper.readValue<LivsoppholdVilkårDTO>(bodyAsText())
                    livsoppholdVilkår.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeFalse()
                    livsoppholdVilkår.avklartSaksopplysning.saksbehandler shouldNotBeNull { this.navIdent shouldBe saksbehandlerIdent }
                }
            }
        }
    }

    @Test
    fun `test at sbh ikke kan si at bruker har livsoppholdytelser`() {
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns mockSaksbehandler

        val objectMotherSak = ObjectMother.sakMedOpprettetBehandling(løpenummer = 1015)
        val behandlingId = objectMotherSak.behandlinger.first().id.toString()
        val vurderingsPeriode = objectMotherSak.behandlinger.first().vurderingsperiode

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
                sakRepo = testDataHelper.sakRepo,
                attesteringRepo = testDataHelper.attesteringRepo,
                sessionFactory = testDataHelper.sessionFactory,
            )
            val livsoppholdVilkårService = LivsoppholdVilkårServiceImpl(
                behandlingRepo = testDataHelper.behandlingRepo,
                behandlingService = behandlingService,
            )

            testApplication {
                application {
                    configureExceptions()
                    jacksonSerialization()
                    routing {
                        livsoppholdRoutes(
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            livsoppholdVilkårService = livsoppholdVilkårService,
                            behandlingService = behandlingService,
                        )
                    }
                }

                // Sjekk at man ikke kan si at bruker har livsoppholdytelser
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/livsopphold")
                    },
                ) {
                    setBody(bodyLivsoppholdYtelse(vurderingsPeriode.toDTO(), true))
                }.apply {
                    status shouldBe HttpStatusCode.NotImplemented
                }
            }
        }
    }

    @Test
    fun `test alle livsoppholdytelser stemmer overens med søknadsdata`() {
        every { mockInnloggetSaksbehandlerProvider.krevInnloggetSaksbehandler(any()) } returns mockSaksbehandler

        val sakId = SakId.random()
        val søknadMedSykepenger = nySøknad(
            sykepenger = periodeJa(fom = 1.januar(2023), tom = 31.mars(2023)),
        )

        val livsoppholdVilkårSykepenger = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedSykepenger, 1011)
        livsoppholdVilkårSykepenger.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårSykepenger.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedEtterlønn = nySøknad(
            etterlønn = ja(),
        )
        val livsoppholdVilkårEtterlønn = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedEtterlønn, 1005)
        livsoppholdVilkårEtterlønn.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårEtterlønn.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedGjenlevendepensjon = nySøknad(
            gjenlevendepensjon = periodeJa(fom = 1.januar(2023), tom = 31.mars(2023)),
        )
        val livsoppholdVilkårGjenlevendepensjon = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedGjenlevendepensjon, 1006)
        livsoppholdVilkårGjenlevendepensjon.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårGjenlevendepensjon.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedSuAlder = nySøknad(
            supplerendeStønadAlder = periodeJa(fom = 1.januar(2023), tom = 31.mars(2023)),
        )
        val livsoppholdVilkårSuAlder = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedSuAlder, 1007)
        livsoppholdVilkårSuAlder.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårSuAlder.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedSuflykning = nySøknad(
            supplerendeStønadFlyktning = periodeJa(fom = 1.januar(2023), tom = 31.mars(2023)),
        )
        val livsoppholdVilkårSuflykning = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedSuflykning, 1008)
        livsoppholdVilkårSuflykning.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårSuflykning.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedJobbsjansen = nySøknad(
            jobbsjansen = periodeJa(fom = 1.januar(2023), tom = 31.mars(2023)),
        )
        val livsoppholdVilkårJobbsjansen = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedJobbsjansen, 1009)
        livsoppholdVilkårJobbsjansen.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårJobbsjansen.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedPensjonsinntekt = nySøknad(
            trygdOgPensjon = periodeJa(fom = 1.januar(2023), tom = 31.mars(2023)),
        )
        val livsoppholdVilkårPensjonsinntekt = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedPensjonsinntekt, 1010)
        livsoppholdVilkårPensjonsinntekt.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårPensjonsinntekt.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT

        val søknadMedAlderpensjon = nySøknad(
            alderspensjon = fraOgMedDatoJa(fom = 1.januar(2023)),
        )
        val livsoppholdVilkårAlderpensjon = opprettSakOgKjørGetPåLivsopphold(sakId, søknadMedAlderpensjon, 1011)
        livsoppholdVilkårAlderpensjon.avklartSaksopplysning.harLivsoppholdYtelser.shouldBeTrue()
        livsoppholdVilkårAlderpensjon.samletUtfall shouldBe SamletUtfallDTO.IKKE_OPPFYLT
    }

    private fun opprettSakOgKjørGetPåLivsopphold(sakId: SakId, søknad: Søknad, løpenummer: Int): LivsoppholdVilkårDTO {
        val registrerteTiltak = listOf(
            ObjectMother.tiltak(),
        )

        val objectMotherSak = ObjectMother.sakMedOpprettetBehandling(id = sakId, behandlinger = listOf(Førstegangsbehandling.opprettBehandling(sakId, søknad, registrerteTiltak, personopplysningFødselsdato())), løpenummer = løpenummer)
        val behandlingId = objectMotherSak.behandlinger.first().id.toString()

        lateinit var livsoppholdVilkårDTO: LivsoppholdVilkårDTO

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
                sakRepo = testDataHelper.sakRepo,
                attesteringRepo = testDataHelper.attesteringRepo,
                sessionFactory = testDataHelper.sessionFactory,
            )
            val livsoppholdVilkårService = LivsoppholdVilkårServiceImpl(
                behandlingRepo = testDataHelper.behandlingRepo,
                behandlingService = behandlingService,
            )

            testApplication {
                application {
                    configureExceptions()
                    jacksonSerialization()
                    routing {
                        livsoppholdRoutes(
                            innloggetSaksbehandlerProvider = mockInnloggetSaksbehandlerProvider,
                            livsoppholdVilkårService = livsoppholdVilkårService,
                            behandlingService = behandlingService,
                        )
                    }
                }

                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$behandlingPath/$behandlingId/vilkar/livsopphold")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.OK
                    livsoppholdVilkårDTO = objectMapper.readValue<LivsoppholdVilkårDTO>(bodyAsText())
                }
            }
        }

        return livsoppholdVilkårDTO
    }

    private fun bodyLivsoppholdYtelse(periodeDTO: PeriodeDTO, harYtelse: Boolean): String {
        val harYtelseString = if (harYtelse) "true" else "false"
        return """
        {
          "ytelseForPeriode": 
            {
              "periode": {
                "fraOgMed": "${periodeDTO.fraOgMed}",
                "tilOgMed": "${periodeDTO.tilOgMed}"
              },
              "harYtelse": $harYtelseString
            }
        }
        """.trimIndent()
    }
}