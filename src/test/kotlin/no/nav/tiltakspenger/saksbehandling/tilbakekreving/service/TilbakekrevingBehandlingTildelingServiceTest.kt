package no.nav.tiltakspenger.saksbehandling.tilbakekreving.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class TilbakekrevingBehandlingTildelingServiceTest {

    private val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
    private lateinit var tilbakekrevingRepo: TilbakekrevingBehandlingFakeRepo
    private lateinit var sakService: SakService
    private lateinit var service: TilbakekrevingBehandlingTildelingService

    private val sakId = SakId.random()
    private val fnr = Fnr.fromString("12345678911")
    private val tilbakekrevingId = TilbakekrevingId.random()
    private val utbetalingId = UtbetalingId.random()

    private fun lagTilbakekrevingBehandling(
        status: TilbakekrevingBehandlingsstatus = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
        saksbehandlerIdent: String? = null,
        beslutterIdent: String? = null,
    ) = TilbakekrevingBehandling(
        id = tilbakekrevingId,
        sakId = sakId,
        utbetalingId = utbetalingId,
        tilbakeBehandlingId = "eksternId",
        opprettet = LocalDateTime.of(2025, 1, 1, 0, 0),
        sistEndret = LocalDateTime.of(2025, 1, 1, 0, 0),
        status = status,
        url = "http://tilbakekreving/123",
        kravgrunnlagTotalPeriode = Periode(fraOgMed = 1.januar(2025), tilOgMed = 31.januar(2025)),
        totaltFeilutbetaltBeløp = BigDecimal("1000"),
        varselSendt = null,
        saksbehandlerIdent = saksbehandlerIdent,
        beslutterIdent = beslutterIdent,
    )

    private fun opprettSakMedTilbakekreving(behandling: TilbakekrevingBehandling): Sak {
        val sak = Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
            behandlinger = Behandlinger(
                rammebehandlinger = Rammebehandlinger.empty(),
                meldekortbehandlinger = Meldekortbehandlinger.empty(),
                klagebehandlinger = Klagebehandlinger.empty(),
            ),
            vedtaksliste = Vedtaksliste.empty(),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            søknader = emptyList(),
            tilbakekrevinger = listOf(behandling),
            kanSendeInnHelgForMeldekort = false,
        )
        return sak
    }

    @BeforeEach
    fun setup() {
        tilbakekrevingRepo = TilbakekrevingBehandlingFakeRepo()

        val behandlingFakeRepo = RammebehandlingFakeRepo()
        val sakFakeRepo = no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakFakeRepo(
            behandlingRepo = behandlingFakeRepo,
            rammevedtakRepo = no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakFakeRepo(
                no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingFakeRepo(),
            ),
            meldekortBehandlingRepo = no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingFakeRepo(),
            meldeperiodeRepo = no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeFakeRepo(),
            meldekortvedtakRepo = no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakFakeRepo(
                no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingFakeRepo(),
            ),
            klagevedtakRepo = no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakFakeRepo(),
            søknadFakeRepo = no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo(behandlingFakeRepo),
            klagebehandlingFakeRepo = no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingFakeRepo(),
            brukersMeldekortFakeRepo = no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortFakeRepo(
                no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeFakeRepo(),
            ),
            tilbakekrevingBehandlingFakeRepo = tilbakekrevingRepo,
            clock = clock,
        )

        sakService = SakService(
            sakRepo = sakFakeRepo,
            personService = io.mockk.mockk(),
            fellesSkjermingsklient = io.mockk.mockk(),
            sessionFactory = no.nav.tiltakspenger.libs.common.TestSessionFactory(),
        )

        service = TilbakekrevingBehandlingTildelingService(
            sakService = sakService,
            tilbakekrevingBehandlingRepo = tilbakekrevingRepo,
            clock = clock,
        )

        // Opprett sak og lagre tilbakekreving i repo
        val behandling = lagTilbakekrevingBehandling()
        val sak = opprettSakMedTilbakekreving(behandling)
        sakFakeRepo.data.get()[sakId] = sak
        tilbakekrevingRepo.lagre(behandling)
    }

    @Nested
    inner class TaBehandling {

        @Test
        fun `saksbehandler kan ta behandling med status TIL_BEHANDLING`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S123456")

            val (sak, oppdatert) = service.taBehandling(sakId, tilbakekrevingId, saksbehandler)

            oppdatert.saksbehandlerIdent shouldBe "S123456"
            oppdatert.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatert.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
            sak.tilbakekrevinger.single().saksbehandlerIdent shouldBe "S123456"
        }

        @Test
        fun `beslutter kan ta behandling med status TIL_GODKJENNING`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")
            val beslutter = ObjectMother.saksbehandler(navIdent = "B222222")

            // Sett opp en behandling som er TIL_GODKJENNING med saksbehandler satt
            val tilGodkjenning = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                saksbehandlerIdent = saksbehandler.navIdent,
            )
            tilbakekrevingRepo.lagre(tilGodkjenning)

            val (sak, oppdatert) = service.taBehandling(sakId, tilbakekrevingId, beslutter)

            oppdatert.beslutterIdent shouldBe "B222222"
            oppdatert.saksbehandlerIdent shouldBe "S111111"
            oppdatert.status shouldBe TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
            oppdatert.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING
        }

        @Test
        fun `kan ikke ta behandling som allerede er tatt (UNDER_BEHANDLING)`() {
            val saksbehandler1 = ObjectMother.saksbehandler(navIdent = "S111111")
            val saksbehandler2 = ObjectMother.saksbehandler(navIdent = "S222222")

            // Ta behandlingen først
            service.taBehandling(sakId, tilbakekrevingId, saksbehandler1)

            shouldThrow<IllegalStateException> {
                service.taBehandling(sakId, tilbakekrevingId, saksbehandler2)
            }
        }

        @Test
        fun `kan ikke ta behandling med status OPPRETTET`() {
            val opprettet = lagTilbakekrevingBehandling(status = TilbakekrevingBehandlingsstatus.OPPRETTET)
            tilbakekrevingRepo.lagre(opprettet)
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")

            shouldThrow<IllegalArgumentException> {
                service.taBehandling(sakId, tilbakekrevingId, saksbehandler)
            }
        }

        @Test
        fun `kan ikke ta behandling med status AVSLUTTET`() {
            val avsluttet = lagTilbakekrevingBehandling(status = TilbakekrevingBehandlingsstatus.AVSLUTTET)
            tilbakekrevingRepo.lagre(avsluttet)
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")

            shouldThrow<IllegalArgumentException> {
                service.taBehandling(sakId, tilbakekrevingId, saksbehandler)
            }
        }

        @Test
        fun `beslutter kan ikke ta behandling der beslutter er lik saksbehandler`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")
            val tilGodkjenning = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                saksbehandlerIdent = saksbehandler.navIdent,
            )
            tilbakekrevingRepo.lagre(tilGodkjenning)

            shouldThrow<IllegalStateException> {
                service.taBehandling(sakId, tilbakekrevingId, saksbehandler)
            }
        }
    }

    @Nested
    inner class OvertaBehandling {

        @Test
        fun `saksbehandler kan overta behandling med status UNDER_BEHANDLING`() {
            val saksbehandler1 = ObjectMother.saksbehandler(navIdent = "S111111")
            val saksbehandler2 = ObjectMother.saksbehandler(navIdent = "S222222")

            // Saksbehandler 1 tar behandlingen
            service.taBehandling(sakId, tilbakekrevingId, saksbehandler1)

            // Saksbehandler 2 overtar
            val (sak, oppdatert) = service.overtaBehandling(sakId, tilbakekrevingId, saksbehandler2)

            oppdatert.saksbehandlerIdent shouldBe "S222222"
            oppdatert.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatert.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
        }

        @Test
        fun `beslutter kan overta behandling med status UNDER_GODKJENNING`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")
            val beslutter1 = ObjectMother.saksbehandler(navIdent = "B111111")
            val beslutter2 = ObjectMother.saksbehandler(navIdent = "B222222")

            // Sett opp behandling som er TIL_GODKJENNING med saksbehandler, ta som beslutter1
            val tilGodkjenning = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                saksbehandlerIdent = saksbehandler.navIdent,
            )
            tilbakekrevingRepo.lagre(tilGodkjenning)
            service.taBehandling(sakId, tilbakekrevingId, beslutter1)

            // Beslutter 2 overtar
            val (sak, oppdatert) = service.overtaBehandling(sakId, tilbakekrevingId, beslutter2)

            oppdatert.beslutterIdent shouldBe "B222222"
            oppdatert.saksbehandlerIdent shouldBe "S111111"
            oppdatert.status shouldBe TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
            oppdatert.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING
        }

        @Test
        fun `kan ikke overta fra seg selv`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")
            service.taBehandling(sakId, tilbakekrevingId, saksbehandler)

            shouldThrow<IllegalArgumentException> {
                service.overtaBehandling(sakId, tilbakekrevingId, saksbehandler)
            }
        }

        @Test
        fun `kan ikke overta behandling som ikke er tatt`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")

            shouldThrow<IllegalArgumentException> {
                service.overtaBehandling(sakId, tilbakekrevingId, saksbehandler)
            }
        }
    }

    @Nested
    inner class LeggTilbakeBehandling {

        @Test
        fun `saksbehandler kan legge tilbake behandling med status UNDER_BEHANDLING`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")
            service.taBehandling(sakId, tilbakekrevingId, saksbehandler)

            val (sak, oppdatert) = service.leggTilbakeBehandling(sakId, tilbakekrevingId, saksbehandler)

            oppdatert.saksbehandlerIdent shouldBe null
            oppdatert.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatert.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING
        }

        @Test
        fun `beslutter kan legge tilbake behandling med status UNDER_GODKJENNING`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")
            val beslutter = ObjectMother.saksbehandler(navIdent = "B222222")

            val tilGodkjenning = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                saksbehandlerIdent = saksbehandler.navIdent,
            )
            tilbakekrevingRepo.lagre(tilGodkjenning)
            service.taBehandling(sakId, tilbakekrevingId, beslutter)

            val (sak, oppdatert) = service.leggTilbakeBehandling(sakId, tilbakekrevingId, beslutter)

            oppdatert.beslutterIdent shouldBe null
            oppdatert.saksbehandlerIdent shouldBe "S111111"
            oppdatert.status shouldBe TilbakekrevingBehandlingsstatus.TIL_GODKJENNING
            oppdatert.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING
        }

        @Test
        fun `kan ikke legge tilbake behandling som en annen saksbehandler eier`() {
            val saksbehandler1 = ObjectMother.saksbehandler(navIdent = "S111111")
            val saksbehandler2 = ObjectMother.saksbehandler(navIdent = "S222222")
            service.taBehandling(sakId, tilbakekrevingId, saksbehandler1)

            shouldThrow<IllegalArgumentException> {
                service.leggTilbakeBehandling(sakId, tilbakekrevingId, saksbehandler2)
            }
        }

        @Test
        fun `kan ikke legge tilbake behandling som ikke er tatt`() {
            val saksbehandler = ObjectMother.saksbehandler(navIdent = "S111111")

            shouldThrow<IllegalArgumentException> {
                service.leggTilbakeBehandling(sakId, tilbakekrevingId, saksbehandler)
            }
        }
    }

    @Nested
    inner class StatusInternDerivering {

        @Test
        fun `statusIntern er TIL_BEHANDLING når status er TIL_BEHANDLING og saksbehandler er null`() {
            val behandling = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
                saksbehandlerIdent = null,
            )
            behandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING
        }

        @Test
        fun `statusIntern er UNDER_BEHANDLING når status er TIL_BEHANDLING og saksbehandler er satt`() {
            val behandling = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
                saksbehandlerIdent = "S111111",
            )
            behandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
        }

        @Test
        fun `statusIntern er TIL_GODKJENNING når status er TIL_GODKJENNING og beslutter er null`() {
            val behandling = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                saksbehandlerIdent = "S111111",
                beslutterIdent = null,
            )
            behandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING
        }

        @Test
        fun `statusIntern er UNDER_GODKJENNING når status er TIL_GODKJENNING og beslutter er satt`() {
            val behandling = lagTilbakekrevingBehandling(
                status = TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                saksbehandlerIdent = "S111111",
                beslutterIdent = "B222222",
            )
            behandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING
        }

        @Test
        fun `statusIntern er OPPRETTET når status er OPPRETTET`() {
            val behandling = lagTilbakekrevingBehandling(status = TilbakekrevingBehandlingsstatus.OPPRETTET)
            behandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.OPPRETTET
        }

        @Test
        fun `statusIntern er AVSLUTTET når status er AVSLUTTET`() {
            val behandling = lagTilbakekrevingBehandling(status = TilbakekrevingBehandlingsstatus.AVSLUTTET)
            behandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.AVSLUTTET
        }
    }

    @Nested
    inner class RoundTrip {

        @Test
        fun `ta-leggTilbake-ta roundtrip bevarer ekstern status`() {
            val saksbehandler1 = ObjectMother.saksbehandler(navIdent = "S111111")
            val saksbehandler2 = ObjectMother.saksbehandler(navIdent = "S222222")

            // Ta
            val (_, etter1) = service.taBehandling(sakId, tilbakekrevingId, saksbehandler1)
            etter1.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            etter1.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING

            // Legg tilbake
            val (_, etter2) = service.leggTilbakeBehandling(sakId, tilbakekrevingId, saksbehandler1)
            etter2.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            etter2.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING

            // En annen saksbehandler tar
            val (_, etter3) = service.taBehandling(sakId, tilbakekrevingId, saksbehandler2)
            etter3.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            etter3.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING
            etter3.saksbehandlerIdent shouldBe "S222222"
        }
    }
}
