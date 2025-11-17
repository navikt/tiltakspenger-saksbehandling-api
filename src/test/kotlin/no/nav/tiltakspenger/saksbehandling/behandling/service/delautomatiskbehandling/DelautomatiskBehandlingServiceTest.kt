package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.util.reflect.instanceOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.ValgteTiltaksdeltakelser
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DelautomatiskBehandlingServiceTest {
    @Test
    fun `behandleAutomatisk - behandling som kan behandles automatisk sendes til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac)
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()
                val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.find { it.eksternDeltagelseId == soknad.tiltak.id }!!
                val virkningsperiode = soknad.tiltaksdeltakelseperiodeDetErSøktOm()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                oppdatertBehandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                oppdatertBehandling.automatiskSaksbehandlet shouldBe true
                oppdatertBehandling.manueltBehandlesGrunner shouldBe emptyList()

                oppdatertBehandling.antallDagerPerMeldeperiode shouldBe Periodisering(AntallDagerForMeldeperiode(10), soknad.tiltaksdeltakelseperiodeDetErSøktOm())
                oppdatertBehandling.resultat!!.instanceOf(BehandlingResultat.Innvilgelse::class) shouldBe true
                oppdatertBehandling.virkningsperiode shouldBe virkningsperiode
                oppdatertBehandling.barnetillegg shouldBe Barnetillegg.utenBarnetillegg(virkningsperiode)
                oppdatertBehandling.valgteTiltaksdeltakelser shouldBe ValgteTiltaksdeltakelser(
                    SammenhengendePeriodisering(
                        tiltaksdeltakelse,
                        virkningsperiode,
                    ),
                )
            }
        }
    }

    @Test
    fun `behandleAutomatisk - behandling der deltakelsen ikke har startet settes på vent`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val virkningsperiode = Periode(fraOgMed = LocalDate.now().plusDays(3), tilOgMed = LocalDate.now().plusMonths(3))
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                    tac = tac,
                    virkningsperiode = virkningsperiode,
                    tiltaksdeltakelse = ObjectMother.tiltaksdeltagelseTac(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        status = TiltakDeltakerstatus.VenterPåOppstart,
                    ),
                )
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                    it.venterTil shouldBe null
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                oppdatertBehandling.venterTil?.toLocalDate() shouldBe virkningsperiode.fraOgMed
                oppdatertBehandling.ventestatus.erSattPåVent shouldBe true
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandleAutomatisk - behandling der deltakelsen ikke lenger er på vent kan gjenopptas og behandles`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val virkningsperiode = Periode(fraOgMed = LocalDate.now().minusDays(1), tilOgMed = LocalDate.now().plusMonths(3))
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                    tac = tac,
                    virkningsperiode = virkningsperiode,
                    tiltaksdeltakelse = ObjectMother.tiltaksdeltagelseTac(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        status = TiltakDeltakerstatus.Deltar,
                    ),
                )
                val behandlingPaVent = behandling.settPåVent(
                    endretAv = AUTOMATISK_SAKSBEHANDLER,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    clock = clock,
                    venterTil = virkningsperiode.fraOgMed.atStartOfDay(),
                ) as Søknadsbehandling
                tac.behandlingContext.behandlingRepo.lagre(behandlingPaVent)
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                    it.venterTil shouldNotBe null
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandlingPaVent, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                oppdatertBehandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                oppdatertBehandling.venterTil?.toLocalDate() shouldBe null
                oppdatertBehandling.ventestatus.erSattPåVent shouldBe false
                oppdatertBehandling.automatiskSaksbehandlet shouldBe true
                oppdatertBehandling.manueltBehandlesGrunner shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandleAutomatisk - behandling var på vent og skal fortsatt vente - oppdaterer venter_til`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val virkningsperiode = Periode(fraOgMed = LocalDate.now().plusDays(3), tilOgMed = LocalDate.now().plusMonths(3))
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                    tac = tac,
                    virkningsperiode = virkningsperiode,
                    tiltaksdeltakelse = ObjectMother.tiltaksdeltagelseTac(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        status = TiltakDeltakerstatus.VenterPåOppstart,
                    ),
                )
                val behandlingPaVent = behandling.settPåVent(
                    endretAv = AUTOMATISK_SAKSBEHANDLER,
                    begrunnelse = "Tiltaksdeltakelsen har ikke startet ennå",
                    clock = clock,
                    venterTil = LocalDateTime.now(),
                ) as Søknadsbehandling
                tac.behandlingContext.behandlingRepo.lagre(behandlingPaVent)
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                    it.venterTil?.toLocalDate() shouldBe LocalDate.now()
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandlingPaVent, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                oppdatertBehandling.venterTil?.toLocalDate() shouldBe virkningsperiode.fraOgMed
                oppdatertBehandling.ventestatus.erSattPåVent shouldBe true
                oppdatertBehandling.ventestatus.ventestatusHendelser.size shouldBe 1
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner shouldBe emptyList()
            }
        }
    }

    @Test
    fun `behandleAutomatisk - har ikke deltatt - sendes til manuell behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val virkningsperiode = Periode(fraOgMed = LocalDate.now().minusDays(3), tilOgMed = LocalDate.now().plusMonths(3))
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                    tac = tac,
                    virkningsperiode = virkningsperiode,
                    tiltaksdeltakelse = ObjectMother.tiltaksdeltagelseTac(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        status = TiltakDeltakerstatus.Venteliste,
                    ),
                )
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.venterTil shouldBe null
                oppdatertBehandling.ventestatus.erSattPåVent shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner shouldBe listOf(ManueltBehandlesGrunn.SAKSOPPLYSNING_HAR_IKKE_DELTATT_PA_TILTAK)
            }
        }
    }

    @Test
    fun `behandleAutomatisk - mangler dager per uke og tiltaket er på deltid - sendes til manuell behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val virkningsperiode = Periode(fraOgMed = LocalDate.now().minusDays(1), tilOgMed = LocalDate.now().plusMonths(3))
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                    tac = tac,
                    virkningsperiode = virkningsperiode,
                    tiltaksdeltakelse = ObjectMother.tiltaksdeltagelseTac(
                        fom = virkningsperiode.fraOgMed,
                        tom = virkningsperiode.tilOgMed,
                        status = TiltakDeltakerstatus.Deltar,
                        dagerPrUke = null,
                        prosent = 60.0F,
                        deltidsprosentGjennomforing = 100.0,
                    ),
                )
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.venterTil shouldBe null
                oppdatertBehandling.ventestatus.erSattPåVent shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner shouldBe listOf(ManueltBehandlesGrunn.SAKSOPPLYSNING_DELTIDSTILTAK_UTEN_DAGER_PER_UKE)
            }
        }
    }

    @Test
    fun `behandleAutomatisk - mangler dager per uke men tiltaket er på heltid - sendes til beslutning`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val virkningsperiode = Periode(fraOgMed = LocalDate.now().minusDays(1), tilOgMed = LocalDate.now().plusMonths(3))
                val tiltaksdeltakelse = ObjectMother.tiltaksdeltagelseTac(
                    fom = virkningsperiode.fraOgMed,
                    tom = virkningsperiode.tilOgMed,
                    status = TiltakDeltakerstatus.Deltar,
                    dagerPrUke = null,
                    prosent = null,
                    deltidsprosentGjennomforing = 100.0,
                )
                val (_, soknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                    tac = tac,
                    virkningsperiode = virkningsperiode,
                    tiltaksdeltakelse = tiltaksdeltakelse,
                )
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                }

                soknad.shouldBeInstanceOf<InnvilgbarSøknad>()

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
                oppdatertBehandling.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                oppdatertBehandling.automatiskSaksbehandlet shouldBe true
                oppdatertBehandling.manueltBehandlesGrunner shouldBe emptyList()

                oppdatertBehandling.antallDagerPerMeldeperiode shouldBe Periodisering(AntallDagerForMeldeperiode(10), soknad.tiltaksdeltakelseperiodeDetErSøktOm())
                oppdatertBehandling.resultat!!.instanceOf(BehandlingResultat.Innvilgelse::class) shouldBe true
                oppdatertBehandling.virkningsperiode shouldBe virkningsperiode
                oppdatertBehandling.barnetillegg shouldBe Barnetillegg.utenBarnetillegg(virkningsperiode)
                oppdatertBehandling.valgteTiltaksdeltakelser shouldBe ValgteTiltaksdeltakelser(
                    SammenhengendePeriodisering(
                        tiltaksdeltakelse,
                        virkningsperiode,
                    ),
                )
            }
        }
    }

    @Test
    fun `behandleAutomatisk - har andre ytelser (søknad) - manuell behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val sak = ObjectMother.nySak()
                tac.sakContext.sakRepo.opprettSak(sak)
                val periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023))
                val sakOgBehandling = ObjectMother.sakMedOpprettetAutomatiskBehandling(
                    sakId = sak.id,
                    fnr = sak.fnr,
                    saksnummer = sak.saksnummer,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        fnr = sak.fnr,
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        sykepenger = Søknad.PeriodeSpm.Ja(fraOgMed = periode.fraOgMed, tilOgMed = periode.tilOgMed),
                    ),
                )
                val behandling = sakOgBehandling.second
                tac.søknadContext.søknadRepo.lagre(behandling.søknad)
                tac.behandlingContext.behandlingRepo.lagre(behandling)
                tac.leggTilPerson(
                    fnr = sak.fnr,
                    person = ObjectMother.personopplysningKjedeligFyr(sak.fnr),
                    tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first(),
                )

                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                    (it as Søknadsbehandling).søknad.sykepenger.erJa() shouldBe true
                }

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner shouldBe listOf(ManueltBehandlesGrunn.SOKNAD_HAR_ANDRE_YTELSER)
            }
        }
    }

    @Test
    fun `behandleAutomatisk - har åpen behandling - manuell behandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val fnr = Fnr.random()
                opprettSøknadsbehandlingKlarTilBehandling(tac, fnr = fnr)

                val (_, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac, fnr = fnr)
                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                }

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner shouldBe listOf(ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING)
            }
        }
    }

    @Test
    fun `behandleAutomatisk - flere grunner til manuell behandling - manuell behandling, alle grunner ligger i listen`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val sak = ObjectMother.nySak()
                tac.sakContext.sakRepo.opprettSak(sak)
                val periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023))
                val sakOgBehandling = ObjectMother.sakMedOpprettetAutomatiskBehandling(
                    sakId = sak.id,
                    fnr = sak.fnr,
                    saksnummer = sak.saksnummer,
                    søknad = ObjectMother.nyInnvilgbarSøknad(
                        fnr = sak.fnr,
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        kvp = Søknad.PeriodeSpm.Ja(fraOgMed = periode.fraOgMed, tilOgMed = periode.tilOgMed),
                        institusjon = Søknad.PeriodeSpm.Ja(fraOgMed = periode.fraOgMed, tilOgMed = periode.tilOgMed),
                    ),
                )
                val behandling = sakOgBehandling.second
                tac.søknadContext.søknadRepo.lagre(behandling.søknad)
                tac.behandlingContext.behandlingRepo.lagre(behandling)
                tac.leggTilPerson(
                    fnr = sak.fnr,
                    person = ObjectMother.personopplysningKjedeligFyr(sak.fnr),
                    tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first(),
                )

                tac.behandlingContext.behandlingRepo.hent(behandling.id).also {
                    it.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
                    it.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER_ID
                    (it as Søknadsbehandling).søknad.kvp.erJa() shouldBe true
                    it.søknad.institusjon.erJa() shouldBe true
                }
                opprettSøknadsbehandlingKlarTilBehandling(tac, fnr = sak.fnr)

                tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id) as Søknadsbehandling
                oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
                oppdatertBehandling.saksbehandler shouldBe null
                oppdatertBehandling.automatiskSaksbehandlet shouldBe false
                oppdatertBehandling.manueltBehandlesGrunner.size shouldBe 3
                oppdatertBehandling.manueltBehandlesGrunner.find { it == ManueltBehandlesGrunn.ANNET_APEN_BEHANDLING } shouldNotBe null
                oppdatertBehandling.manueltBehandlesGrunner.find { it == ManueltBehandlesGrunn.SOKNAD_HAR_KVP } shouldNotBe null
                oppdatertBehandling.manueltBehandlesGrunner.find { it == ManueltBehandlesGrunn.SOKNAD_INSTITUSJONSOPPHOLD } shouldNotBe null
            }
        }
    }
}
