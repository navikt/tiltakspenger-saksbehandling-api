package no.nav.tiltakspenger.saksbehandling.meldekort.infra.jobb

import arrow.core.nonEmptyListOf
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.AvbrytMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandlingIverksatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.nyOpprettetMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.httpKlientUventetStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.søknadsbehandlingIverksattMedMeldeperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simuleringsdag
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AutomatiskMeldekortbehandlingJobbTest {
    val clock = TikkendeKlokke(fixedClockAt(1.april(2025).atTime(12, 0)))
    val vedtaksperiode = Periode(6.januar(2025), 31.mars(2025))

    /**
     * Grunnsettet for [no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingPostgresRepo] sin mapping av [MeldekortBehandletAutomatisk] og brukers meldekort.
     * Derfor kjører den mot postgres, mens resten av fila kjører in-memory.
     * Hele tilstanden bygges gjennom prodstiene med [iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk].
     *
     * Isolert fordi jobben henter brukers meldekort på tvers av alle saker.
     * Mot det delte skjemaet ville den behandlet meldekort andre tester har lagt inn.
     *
     * Klokka må stå på en hverdag innenfor økonomisystemets åpningstider, ellers hopper jobben over alle meldekort.
     * Standardklokka (1. mai) er både helligdag og utenfor åpningstidene.
     */
    @Test
    @IsolatedDatabaseTest
    fun `skal behandle brukers meldekort automatisk ved behandlesAutomatisk=true`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak, brukersMeldekort) = iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk(tac = tac)

            val meldekortbehandlinger = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!
            val sisteMeldekortbehandling = meldekortbehandlinger.sisteGodkjenteMeldekort!!

            sisteMeldekortbehandling.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            sisteMeldekortbehandling.brukersMeldekort.id shouldBe brukersMeldekort.id
            sisteMeldekortbehandling.brukersMeldekort.behandletAv shouldBe sisteMeldekortbehandling.id
            // Team Sak vil bare ha de manuelle meldekortbehandlingene i saksstatistikken.
            tac.sessionFactory.hentSaksstatistikk(sak.id).none { it.behandlingType == StatistikkBehandlingType.MELDEKORT } shouldBe true

            // En annen sak der bruker melder tre sammenhengende dager med godkjent fravær.
            // Jobben avviser meldekortet og skriver statusen tilbake på raden — grunnsettet for `oppdaterAutomatiskBehandletStatus`.
            val (sakMedForMyeFravær) = iverksettSøknadsbehandling(tac = tac)
            val meldeperiode = sakMedForMyeFravær.meldeperiodeKjeder.first().hentSisteMeldeperiode()
            val utfylt = meldeperiode.tilUtfyltFraBruker(
                kanSendeInnHelgForMeldekort = sakMedForMyeFravær.kanSendeInnHelgForMeldekort,
            )
            // Bruker kan bare registrere på dager med rett, så fraværet må legges på dager hen faktisk kan fylle ut.
            val fraværsdager = utfylt.keys
                .filter { utfylt.getValue(it) != BrukerutfyltMeldekortDTO.Status.IKKE_BESVART }
                .sorted()
                .windowed(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
                .first { vindu -> vindu.zipWithNext().all { (a, b) -> b == a.plusDays(1) } }
                .toSet()
            val dagerMedFravær = utfylt.mapValues { (dato, status) ->
                if (dato in fraværsdager) BrukerutfyltMeldekortDTO.Status.FRAVÆR_GODKJENT_AV_NAV else status
            }

            val (_, meldekortMedForMyeFravær) = mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = meldeperiode.id,
                sakId = sakMedForMyeFravær.id,
                dager = dagerMedFravær,
                // Journalpost-IDen er unik per meldekort, og standardverdien er allerede brukt av saken over.
                journalpostId = "4321",
            )
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            val lagret = tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(meldekortMedForMyeFravær!!.id)!!
            lagret.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
            // Markert som ikke-automatisk, slik at jobben ikke plukker den opp igjen.
            lagret.behandlesAutomatisk shouldBe false
            // Første innsending i kjeden er ikke en korrigering, og prioriteres ikke.
            lagret.prioritering shouldBe null
            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sakMedForMyeFravær.id) shouldBe null
        }
    }

    @Test
    fun `skal ikke behandle automatisk ved behandlesAutomatisk=false`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingJobb = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak =
                    tac.søknadsbehandlingIverksattMedMeldeperioder(
                        periode = vedtaksperiode,
                        clock = clock,
                    )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = false,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)

                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)

                meldekortbehandlinger shouldBe null
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk med for mange dager registrert`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->

                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingJobb =
                    tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val sak =
                    tac.søknadsbehandlingIverksattMedMeldeperioder(
                        periode = vedtaksperiode,
                        clock = clock,
                    )

                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
                val førsteDag = meldeperiode.periode.fraOgMed

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    dager = List(14) {
                        BrukersMeldekort.BrukersMeldekortDag(
                            dato = førsteDag.plusDays(it.toLong()),
                            status = if (it < 11) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                        )
                    },
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)
                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)

                meldekortbehandlinger shouldBe null
            }
        }
    }

    @Test
    fun `skal kun behandle det neste meldekortet på en sak for hvert kall`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingJobb = tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val sak =
                    tac.søknadsbehandlingIverksattMedMeldeperioder(
                        periode = vedtaksperiode,
                        clock = clock,
                    )

                val brukersMeldekort1 = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )

                val brukersMeldekort2 = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder[1].hentSisteMeldeperiode(),
                )

                brukersMeldekortRepo.lagre(brukersMeldekort1)
                brukersMeldekortRepo.lagre(brukersMeldekort2)

                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!

                meldekortbehandlinger.godkjenteMeldekort.size shouldBe 1
                meldekortbehandlinger.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                (meldekortbehandlinger.sisteGodkjenteMeldekort as MeldekortBehandletAutomatisk).brukersMeldekort.id shouldBe brukersMeldekort1.id

                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock.plus(
                        1,
                        ChronoUnit.MINUTES,
                    ),
                )

                val meldekortbehandlingerNeste = meldekortbehandlingRepo.hentForSakId(sak.id)!!

                meldekortbehandlingerNeste.godkjenteMeldekort.size shouldBe 2
                meldekortbehandlingerNeste.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                (meldekortbehandlingerNeste.sisteGodkjenteMeldekort as MeldekortBehandletAutomatisk).brukersMeldekort.id shouldBe brukersMeldekort2.id
            }
        }
    }

    @Test
    fun `skal ikke behandle korrigering automatisk etter stengetid neste virkedag etter at første meldekort ble behandlet`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingJobb =
                    tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val sak =
                    tac.søknadsbehandlingIverksattMedMeldeperioder(
                        periode = vedtaksperiode,
                        clock = clock,
                    )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)
                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock)

                // Første meldekort ble behandlet tirsdag 1. april, så fristen for automatisk korrigering er onsdag 2. april kl 20:50.
                // Det er behandlingstidspunktet som teller, så korrigeringen avvises selv om den ble mottatt før fristen.
                val brukersMeldekortDuplikat = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = 2.april(2025).atTime(16, 0),
                )

                brukersMeldekortRepo.lagre(brukersMeldekortDuplikat)

                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(fixedClockAt(3.april(2025).atTime(8, 0)))

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)

                meldekortbehandlinger!!.godkjenteMeldekort.size shouldBe 1

                brukersMeldekortRepo
                    .hentForMeldekortId(brukersMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
                brukersMeldekortRepo
                    .hentForMeldekortId(brukersMeldekortDuplikat.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
            }
        }
    }

    @Test
    fun `skal ikke behandle brukers meldekort automatisk utenfor økonomisystemets åpningstider`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingJobb =
                    tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val nå = LocalDate.now(clock)
                val mandag = nå.with(DayOfWeek.MONDAY)

                val sak =
                    tac.søknadsbehandlingIverksattMedMeldeperioder(
                        periode = vedtaksperiode,
                        clock = clock,
                    )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)

                for (dagOffset in DayOfWeek.MONDAY.ordinal..DayOfWeek.FRIDAY.ordinal) {
                    val dag = mandag.plusDays(dagOffset.toLong())
                    val klokken0559 = fixedClockAt(dag.atTime(5, 59))
                    val klokken2100 = fixedClockAt(dag.atTime(21, 0))
                    val klokken2101 = fixedClockAt(dag.atTime(21, 1))

                    automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = klokken0559)
                    automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = klokken2100)
                    automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = klokken2101)
                }

                // Lørdag kan økonomisystemet være åpent 07:10-17:50, så vi tester bare tidspunktene utenfor.
                val lørdag = mandag.with(DayOfWeek.SATURDAY)
                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = fixedClockAt(lørdag.atTime(7, 9)))
                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = fixedClockAt(lørdag.atTime(17, 50)))

                val søndag = mandag.with(DayOfWeek.SUNDAY)
                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = fixedClockAt(søndag.atTime(12, 0)))

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            }
        }
    }

    @Test
    fun `skal behandle meldekort automatisk når økonomisystemet muligens er åpent og simuleringen går gjennom`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                tac.meldekortContext.brukersMeldekortRepo.lagre(brukersMeldekort)

                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = muligÅpentKlokke(),
                )

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.sisteGodkjenteMeldekort!!
                    .shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                    .simulering shouldNotBe null
                tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            }
        }
    }

    @Test
    fun `skal sette jobben på pause i 15 minutter uten å lagre forsøket når økonomisystemet muligens er åpent og simuleringen svarer stengt`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.Stengt(
                    httpKlientUventetStatus(statusCode = 503, body = "simulering stengt"),
                )
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = muligÅpentKlokke(),
                )

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
                // Forsøket lagres ikke, slik at det ikke teller mot maksgrensen for nye forsøk.
                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandlesAutomatisk shouldBe true
                    it.behandletAutomatiskStatus shouldBe brukersMeldekort.behandletAutomatiskStatus
                    it.behandletAutomatiskForsøkshistorikk shouldBe brukersMeldekort.behandletAutomatiskForsøkshistorikk
                }

                // Simuleringen virker igjen, men jobben står på pause til 15 minutter etter at den svarte stengt.
                tac.utbetalingFakeKlient.simulerFeil = null
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = fixedClockAt(1.april(2025).atTime(18, 14, 59)),
                )
                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = fixedClockAt(1.april(2025).atTime(18, 15)),
                )
                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.sisteGodkjenteMeldekort!!
                    .shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                    .brukersMeldekort.id shouldBe brukersMeldekort.id
            }
        }
    }

    @Test
    fun `skal vente uten å lagre forsøket mens saken migreres til oppdragsversjon 3`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.SakenMigreres
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = clock)

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.behandletAutomatiskForsøkshistorikk shouldBe
                    brukersMeldekort.behandletAutomatiskForsøkshistorikk

                tac.utbetalingFakeKlient.simulerFeil = null
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = clock)
                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.sisteGodkjenteMeldekort!!
                    .shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                    .brukersMeldekort.id shouldBe brukersMeldekort.id
            }
        }
    }

    @Test
    fun `skal kreve simulering for førstegangsbehandling både når økonomisystemet muligens og antatt er åpent`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.UkjentFeil(
                    httpKlientUventetStatus(statusCode = 500, body = "simulering feilet"),
                )
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = muligÅpentKlokke(),
                )

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET
                    it.behandlesAutomatisk shouldBe true
                }

                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = fixedClockAt(2.april(2025).atTime(8, 0)),
                )

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET
                    it.behandlesAutomatisk shouldBe true
                }

                tac.utbetalingFakeKlient.simulerFeil = null
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(
                    clock = fixedClockAt(2.april(2025).atTime(9, 0)),
                )

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.sisteGodkjenteMeldekort!!
                    .shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                    .simulering shouldNotBe null
            }
        }
    }

    /** Tirsdag kl 18, etter antatt stengetid, men før økonomisystemet senest stenger. */
    private fun muligÅpentKlokke() = fixedClockAt(1.april(2025).atTime(18, 0))

    @Test
    fun `skal prøve på nytt etter 1 minutt for første forsøk`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val forrigeForsøk = nå(clock)
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    behandletAutomatiskStatus = MeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
                    behandletAutomatiskForsøkshistorikk = Forsøkshistorikk.opprett(
                        forrigeForsøk = forrigeForsøk,
                        antallForsøk = 1L,
                        clock = clock,
                    ),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                // 59 sekunder etter forrige forsøk - skal hoppe over
                service.behandleBrukersMeldekort(clock = fixedClockAt(forrigeForsøk.plusSeconds(59)))
                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                // 1 minutt og 1 sekund etter forrige forsøk - skal prøve på nytt
                service.behandleBrukersMeldekort(clock = fixedClockAt(forrigeForsøk.plusSeconds(61)))
                meldekortbehandlingRepo.hentForSakId(sak.id)!!
                    .sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            }
        }
    }

    @Test
    fun `skal prøve på nytt etter 5 minutter for antallForsøk = 6`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val forrigeForsøk = nå(clock)
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    behandletAutomatiskStatus = MeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
                    behandletAutomatiskForsøkshistorikk = Forsøkshistorikk.opprett(
                        forrigeForsøk = forrigeForsøk,
                        antallForsøk = 6,
                        clock = clock,
                    ),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                // 1 minutt etter forrige forsøk - skal fortsatt hoppe over (trenger 5 min)
                service.behandleBrukersMeldekort(clock = fixedClockAt(forrigeForsøk.plusMinutes(1)))
                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                // 4 minutter og 59 sekunder etter forrige forsøk - skal fortsatt hoppe over
                service.behandleBrukersMeldekort(clock = fixedClockAt(forrigeForsøk.plusMinutes(4).plusSeconds(59)))
                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                // 5 minutter og 1 sekund etter forrige forsøk - skal prøve på nytt
                service.behandleBrukersMeldekort(clock = fixedClockAt(forrigeForsøk.plusMinutes(5).plusSeconds(1)))
                meldekortbehandlingRepo.hentForSakId(sak.id)!!
                    .sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            }
        }
    }

    @Test
    fun `skal prøve på nytt etter 15 minutter (max delay) for antallForsøk 7 og flere`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val forrigeForsøk = nå(clock)
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    behandletAutomatiskStatus = MeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
                    behandletAutomatiskForsøkshistorikk = Forsøkshistorikk.opprett(
                        forrigeForsøk = forrigeForsøk,
                        antallForsøk = 7L,
                        clock = clock,
                    ),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                // 14 minutter og 59 sekunder etter forrige forsøk - skal fortsatt hoppe over
                service.behandleBrukersMeldekort(
                    clock = fixedClockAt(
                        forrigeForsøk.plusMinutes(14).plusSeconds(59),
                    ),
                )
                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                // 15 minutter og 1 sekund etter forrige forsøk - skal prøve på nytt
                service.behandleBrukersMeldekort(clock = fixedClockAt(forrigeForsøk.plusMinutes(15).plusSeconds(1)))
                meldekortbehandlingRepo.hentForSakId(sak.id)!!
                    .sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            }
        }
    }

    @Test
    fun `skal ikke prøve på nytt ved status som ikke skal retries (feks FOR_SENT_Å_KORRIGERE)`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)
                service.behandleBrukersMeldekort(clock)

                // Duplikat for samme meldeperiode etter at korrigeringsvinduet er omme -> vil feile med FOR_SENT_Å_KORRIGERE
                val duplikat = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    mottatt = 3.april(2025).atTime(12, 0),
                )
                brukersMeldekortRepo.lagre(duplikat)
                val etterKorrigeringsvinduet = fixedClockAt(duplikat.mottatt)
                service.behandleBrukersMeldekort(etterKorrigeringsvinduet)

                val lagretDuplikat = brukersMeldekortRepo.hentForMeldekortId(duplikat.id)!!
                lagretDuplikat.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
                // Skal være markert som ikke-automatisk, slik at den ikke plukkes opp igjen
                lagretDuplikat.behandlesAutomatisk shouldBe false

                // Selv langt frem i tid (godt over maxDelay) skal den ikke retries
                service.behandleBrukersMeldekort(clock = fixedClockAt(nå(etterKorrigeringsvinduet).plusHours(1)))
                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 1
            }
        }
    }

    @Test
    fun `skal ikke behandle brukers meldekort som venter på neste forsøk`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingJobb =
                    tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val nå = nå(clock)

                val sak =
                    tac.søknadsbehandlingIverksattMedMeldeperioder(
                        periode = vedtaksperiode,
                        clock = clock,
                    )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    behandletAutomatiskStatus = MeldekortBehandletAutomatiskStatus.UKJENT_FEIL,
                    behandletAutomatiskForsøkshistorikk = Forsøkshistorikk.opprett(clock = clock)
                        .inkrementer(clock = clock),
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)

                val nesteDag = fixedClockAt(nå.plusSeconds(30))
                automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock = nesteDag)

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            }
        }
    }

    /**
     * Meldekortet mottas fredag 4. april 2025 kl 12, og helgen telles ikke med.
     * Fristen for nye forsøk går derfor ut tirsdag 8. april kl 12.
     */
    @Test
    fun `skal slutte å prøve på nytt etter MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING uten å telle med helgen`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Bruker andre kjede slik at behandlingen faller tilbake på en retry-bar status (MÅ_BEHANDLE_FØRSTE_KJEDE)
                val mottatt = 4.april(2025).atTime(12, 0)
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder[1].hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    mottatt = mottatt,
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                // Innenfor MAKS_DELAY (2 virkedager etter mottatt) -> skal forbli markert for automatisk retry
                service.behandleBrukersMeldekort(clock = fixedClockAt(8.april(2025).atTime(11, 0)))

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
                    it.behandlesAutomatisk shouldBe true
                }

                // Etter MAKS_DELAY (mer enn 2 virkedager siden mottatt) -> skal ikke prøves automatisk på nytt
                service.behandleBrukersMeldekort(clock = fixedClockAt(8.april(2025).atTime(12, 0, 1)))

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
                    it.behandlesAutomatisk shouldBe false
                }
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk ved registrert helg når saken ikke tillater helg`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Saken har kanSendeInnHelgForMeldekort = false som default
                val eksisterendeMeldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                // Erstatt meldeperioden med en som gir rett i helg (men saken tillater ikke helg)
                val meldeperiodeMedHelg = eksisterendeMeldeperiode.copy(
                    id = eksisterendeMeldeperiode.id,
                    girRett = eksisterendeMeldeperiode.periode.tilDager().associateWith { true },
                    maksAntallDagerForMeldeperiode = 14,
                )
                meldeperiodeRepo.lagre(meldeperiodeMedHelg)

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiodeMedHelg,
                    dager = meldeperiodeMedHelg.girRett.entries.mapIndexed { index, entry ->
                        BrukersMeldekort.BrukersMeldekortDag(
                            // Registrer dager inkludert helg
                            status = if (index < 7) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                            dato = entry.key,
                        )
                    },
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)
                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                val lagretMeldekort = brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                lagretMeldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG
                lagretMeldekort.behandlesAutomatisk shouldBe false
            }
        }
    }

    @Test
    fun `skal behandle automatisk ved registrert helg når saken tillater helg`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val sakRepo = tac.sakContext.sakRepo

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Aktiver helg for saken
                sakRepo.oppdaterKanSendeInnHelgForMeldekort(sak.id, true)

                val eksisterendeMeldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                // Erstatt meldeperioden med en som gir rett i helg
                val meldeperiodeMedHelg = eksisterendeMeldeperiode.copy(
                    id = eksisterendeMeldeperiode.id,
                    girRett = eksisterendeMeldeperiode.periode.tilDager().associateWith { true },
                    maksAntallDagerForMeldeperiode = 14,
                )
                meldeperiodeRepo.lagre(meldeperiodeMedHelg)

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiodeMedHelg,
                    dager = meldeperiodeMedHelg.girRett.entries.mapIndexed { index, entry ->
                        BrukersMeldekort.BrukersMeldekortDag(
                            status = if (index < 7) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                            dato = entry.key,
                        )
                    },
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)
                service.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!
                meldekortbehandlinger.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk ved for mange sammenhengende dager godkjent fravær`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val sakRepo = tac.sakContext.sakRepo

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Aktiver helg slik at alle dager gir rett - da kan vi teste fravær uten å utløse helg-sjekken
                sakRepo.oppdaterKanSendeInnHelgForMeldekort(sak.id, true)

                val eksisterendeMeldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
                val meldeperiodeMedHelg = eksisterendeMeldeperiode.copy(
                    id = eksisterendeMeldeperiode.id,
                    girRett = eksisterendeMeldeperiode.periode.tilDager().associateWith { true },
                    maksAntallDagerForMeldeperiode = 14,
                )
                meldeperiodeRepo.lagre(meldeperiodeMedHelg)

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiodeMedHelg,
                    dager = meldeperiodeMedHelg.girRett.entries.chunked(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
                        .flatMapIndexed { index, dager ->
                            dager.map {
                                BrukersMeldekort.BrukersMeldekortDag(
                                    status = if (index % 2 == 0) InnmeldtStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU else InnmeldtStatus.IKKE_BESVART,
                                    dato = it.key,
                                )
                            }
                        },
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)
                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                val lagretMeldekort = brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                lagretMeldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
                lagretMeldekort.behandlesAutomatisk shouldBe false
            }
        }
    }

    @Test
    fun `skal behandle automatisk ved akseptabelt antall sammenhengende dager godkjent fravær`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val meldeperiodeRepo = tac.meldekortContext.meldeperiodeRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb
                val sakRepo = tac.sakContext.sakRepo

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Aktiver helg slik at alle dager gir rett
                sakRepo.oppdaterKanSendeInnHelgForMeldekort(sak.id, true)

                val eksisterendeMeldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
                val meldeperiodeMedHelg = eksisterendeMeldeperiode.copy(
                    id = eksisterendeMeldeperiode.id,
                    girRett = eksisterendeMeldeperiode.periode.tilDager().associateWith { true },
                    maksAntallDagerForMeldeperiode = 14,
                )
                meldeperiodeRepo.lagre(meldeperiodeMedHelg)

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiodeMedHelg,
                    dager = meldeperiodeMedHelg.girRett.entries.chunked(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER)
                        .flatMapIndexed { index, dager ->
                            dager.map {
                                BrukersMeldekort.BrukersMeldekortDag(
                                    status = if (index % 2 == 0) InnmeldtStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU else InnmeldtStatus.IKKE_BESVART,
                                    dato = it.key,
                                )
                            }
                        },
                )

                brukersMeldekortRepo.lagre(brukersMeldekort)
                service.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!
                meldekortbehandlinger.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            }
        }
    }

    /**
     * Første meldekort behandles fredag 4. april 2025.
     * Lørdagen regnes ikke som virkedag, selv om økonomisystemet muligens er åpent da, så fristen er mandag 7. april kl 20:50.
     */
    @Test
    fun `skal hoppe over helgen når fristen for automatisk korrigering beregnes`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 7.april(2025).atTime(10, 0),
            Kjøring(7.april(2025).atTime(20, 49, 59)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.BEHANDLET)
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 7.april(2025).atTime(10, 0),
            Kjøring(7.april(2025).atTime(20, 50)),
            Kjøring(8.april(2025).atTime(8, 0)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
            MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE,
        )
    }

    /**
     * Første meldekort behandles onsdag 16. april 2025.
     * Skjærtorsdag, langfredag, påskeaftens lørdag, 1. og 2. påskedag hoppes over, så fristen er tirsdag 22. april kl 20:50.
     */
    @Test
    fun `skal hoppe over helligdager når fristen for automatisk korrigering beregnes`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 16.april(2025).atTime(12, 0),
            korrigeringMottatt = 19.april(2025).atTime(10, 0),
            Kjøring(22.april(2025).atTime(20, 49, 59)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.BEHANDLET)
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 16.april(2025).atTime(12, 0),
            korrigeringMottatt = 19.april(2025).atTime(10, 0),
            Kjøring(22.april(2025).atTime(20, 50)),
            Kjøring(23.april(2025).atTime(8, 0)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
            MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE,
        )
    }

    /** Første meldekort behandles fredag 4. april 2025, og korrigeringen behandles lørdag 5. april mens økonomisystemet muligens er åpent, før fristen mandag. */
    @Test
    fun `skal behandle korrigering automatisk på lørdagen etter at første meldekort ble behandlet en fredag`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 5.april(2025).atTime(10, 0),
            Kjøring(5.april(2025).atTime(10, 0)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.BEHANDLET)
    }

    /** Første meldekort behandles lørdag 5. april 2025 mens økonomisystemet muligens er åpent, så fristen er mandag 7. april kl 20:50. */
    @Test
    fun `skal regne fristen for automatisk korrigering fra neste åpne dag når første meldekort ble behandlet en lørdag`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 5.april(2025).atTime(10, 0),
            korrigeringMottatt = 7.april(2025).atTime(10, 0),
            Kjøring(7.april(2025).atTime(20, 49, 59)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.BEHANDLET)
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 5.april(2025).atTime(10, 0),
            korrigeringMottatt = 7.april(2025).atTime(10, 0),
            Kjøring(7.april(2025).atTime(20, 50)),
            Kjøring(8.april(2025).atTime(8, 0)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
            MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE,
        )
    }

    /**
     * Tirsdag 1. april 2025 kl 14 er økonomisystemet antatt åpent, så et stengt-svar fra simuleringen setter ikke jobben på pause.
     * Korrigeringen prøves i stedet på nytt etter vanlig ventetid, før pausen på 15 minutter ville vært over.
     */
    @Test
    fun `skal prøve korrigering på nytt uten pause når simuleringen svarer stengt mens økonomisystemet er antatt åpent`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 1.april(2025).atTime(12, 0),
            korrigeringMottatt = 1.april(2025).atTime(13, 55),
            Kjøring(
                tidspunkt = 1.april(2025).atTime(14, 0),
                simulerFeil = KunneIkkeSimulere.Stengt(httpKlientUventetStatus(statusCode = 503, body = "simulering stengt")),
            ),
            Kjøring(1.april(2025).atTime(14, 5)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET,
            MeldekortBehandletAutomatiskStatus.BEHANDLET,
        )
    }

    /** Korrigeringen mottas søndag 6. april 2025, og jobben kjører ikke før økonomisystemet åpner mandag, før fristen mandag kl 20:50. */
    @Test
    fun `skal behandle korrigering mottatt mens økonomisystemet er stengt når det åpner før fristen`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 6.april(2025).atTime(10, 0),
            Kjøring(6.april(2025).atTime(10, 0)),
            Kjøring(7.april(2025).atTime(8, 0)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
            MeldekortBehandletAutomatiskStatus.BEHANDLET,
        )
    }

    /**
     * Fristen er mandag 7. april 2025 kl 20:50, og pausen etter at simuleringen svarte stengt kl 20:40 varer til 20:55.
     * Da er økonomisystemet stengt, og når det åpner tirsdag, har fristen gått ut.
     */
    @Test
    fun `skal ikke behandle korrigering automatisk når pausen etter stengt simulering varer forbi fristen`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 7.april(2025).atTime(20, 40),
            Kjøring(
                tidspunkt = 7.april(2025).atTime(20, 40),
                simulerFeil = KunneIkkeSimulere.Stengt(httpKlientUventetStatus(statusCode = 503, body = "simulering stengt")),
            ),
            Kjøring(7.april(2025).atTime(20, 49, 59)),
            Kjøring(8.april(2025).atTime(8, 0)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
            MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING,
            MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE,
        )
    }

    /** Tirsdag 1. april 2025 kl 18 er økonomisystemet muligens åpent, og fristen er onsdag 2. april kl 20:50. */
    @Test
    fun `skal prøve korrigering på nytt når simuleringen feiler mens økonomisystemet muligens er åpent`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 1.april(2025).atTime(12, 0),
            korrigeringMottatt = 1.april(2025).atTime(17, 55),
            Kjøring(
                tidspunkt = 1.april(2025).atTime(18, 0),
                simulerFeil = KunneIkkeSimulere.UkjentFeil(httpKlientUventetStatus(statusCode = 500, body = "simulering feilet")),
            ),
            Kjøring(2.april(2025).atTime(8, 0)),
        ) shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET,
            MeldekortBehandletAutomatiskStatus.BEHANDLET,
        )
    }

    /**
     * Første meldekort behandles fredag 4. april 2025, og fristen for automatisk korrigering går ut mandag 7. april kl 20:50.
     * Prioriteringen bruker samme frist, men regnet fra når korrigeringen ble mottatt, siden jobben her først gir den opp tirsdag.
     */
    @Test
    fun `skal prioritere korrigering som ikke kan behandles automatisk når den er mottatt før fristen for automatisk korrigering`() {
        korrigerMeldekort(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 7.april(2025).atTime(20, 49, 59),
            Kjøring(8.april(2025).atTime(8, 0)),
        ).single().let {
            it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
            it.behandlesAutomatisk shouldBe false
            it.prioritering!!.begrunnelse.verdi shouldBe
                "Korrigert meldekort ble mottatt kort tid etter forrige meldekort for perioden, men kunne ikke behandles automatisk."
        }
        korrigerMeldekort(
            førsteBehandlingstidspunkt = 4.april(2025).atTime(12, 0),
            korrigeringMottatt = 7.april(2025).atTime(20, 50),
            Kjøring(8.april(2025).atTime(8, 0)),
        ).single().prioritering shouldBe null
    }

    /** Første meldekort behandles onsdag 16. april 2025, så påsken hoppes over og fristen for prioritering er tirsdag 22. april kl 20:50. */
    @Test
    fun `skal hoppe over helligdager når fristen for prioritering av korrigering beregnes`() {
        korrigerMeldekort(
            førsteBehandlingstidspunkt = 16.april(2025).atTime(12, 0),
            korrigeringMottatt = 22.april(2025).atTime(20, 49, 59),
            Kjøring(23.april(2025).atTime(8, 0)),
        ).single().prioritering shouldNotBe null
        korrigerMeldekort(
            førsteBehandlingstidspunkt = 16.april(2025).atTime(12, 0),
            korrigeringMottatt = 22.april(2025).atTime(20, 50),
            Kjøring(23.april(2025).atTime(8, 0)),
        ).single().prioritering shouldBe null
    }

    /** Statusen kan prøves på nytt, så korrigeringen prioriteres ikke før jobben gir den opp ved fristen for automatisk korrigering. */
    @Test
    fun `skal prioritere korrigering mottatt før fristen også når jobben gir den opp etter fristen`() {
        korrigerMeldekort(
            førsteBehandlingstidspunkt = 1.april(2025).atTime(12, 0),
            korrigeringMottatt = 2.april(2025).atTime(10, 0),
            Kjøring(
                tidspunkt = 2.april(2025).atTime(10, 0),
                simulerFeil = KunneIkkeSimulere.UkjentFeil(httpKlientUventetStatus(statusCode = 500, body = "simulering feilet")),
            ),
            Kjøring(3.april(2025).atTime(10, 0)),
        ).map { it.behandletAutomatiskStatus to (it.prioritering != null) } shouldBe listOf(
            MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET to false,
            MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE to true,
        )
    }

    private data class Kjøring(val tidspunkt: LocalDateTime, val simulerFeil: KunneIkkeSimulere? = null)

    /**
     * Behandler første meldekort automatisk og mottar så en korrigering av det.
     * Kjører jobben for hver av [kjøringer] og gir statusen til korrigeringen etter hver kjøring.
     */
    private fun korrigerMeldekortAutomatisk(
        førsteBehandlingstidspunkt: LocalDateTime,
        korrigeringMottatt: LocalDateTime,
        vararg kjøringer: Kjøring,
    ): List<MeldekortBehandletAutomatiskStatus> =
        korrigerMeldekort(førsteBehandlingstidspunkt, korrigeringMottatt, *kjøringer).map { it.behandletAutomatiskStatus }

    /** Som [korrigerMeldekortAutomatisk], men gir korrigeringen slik den er lagret etter hver kjøring. */
    private fun korrigerMeldekort(
        førsteBehandlingstidspunkt: LocalDateTime,
        korrigeringMottatt: LocalDateTime,
        vararg kjøringer: Kjøring,
    ): List<BrukersMeldekort> {
        val lagret = mutableListOf<BrukersMeldekort>()
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val korrigertMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = korrigeringMottatt,
                )
                val førsteMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = førsteBehandlingstidspunkt.minusHours(1),
                    dager = korrigertMeldekort.dager.let { dager ->
                        val glemtDag = dager.first { it.status == InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET }
                        dager.map { if (it == glemtDag) it.copy(status = InnmeldtStatus.IKKE_TILTAKSDAG) else it }
                    },
                )
                brukersMeldekortRepo.lagre(førsteMeldekort)
                service.behandleBrukersMeldekort(fixedClockAt(førsteBehandlingstidspunkt))
                brukersMeldekortRepo.hentForMeldekortId(førsteMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET

                brukersMeldekortRepo.lagre(korrigertMeldekort)
                kjøringer.forEach { (tidspunkt, simulerFeil) ->
                    tac.utbetalingFakeKlient.simulerFeil = simulerFeil
                    service.behandleBrukersMeldekort(fixedClockAt(tidspunkt))
                    lagret.add(brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort.id)!!)
                }
            }
        }
        return lagret
    }

    @Test
    fun `skal behandle korrigert meldekort automatisk før stengetid neste virkedag etter at første meldekort ble behandlet`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val korrigertMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = 3.april(2025).atTime(10, 0),
                )

                // Bruker glemte å melde en dag i førstegangsinnsendingen, og korrigerer den til deltatt.
                val førsteMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = nå(clock),
                    dager = korrigertMeldekort.dager.let { dager ->
                        val glemtDag = dager.first { it.status == InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET }
                        dager.map { if (it == glemtDag) it.copy(status = InnmeldtStatus.IKKE_TILTAKSDAG) else it }
                    },
                )
                brukersMeldekortRepo.lagre(førsteMeldekort)
                // Første meldekort mottas 1. april, men behandles først 2. april, så fristen er 3. april kl 20:50.
                service.behandleBrukersMeldekort(fixedClockAt(2.april(2025).atTime(10, 0)))

                brukersMeldekortRepo.lagre(korrigertMeldekort)
                service.behandleBrukersMeldekort(fixedClockAt(3.april(2025).atTime(20, 49, 59)))

                val behandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort
                behandlinger.size shouldBe 2

                val korrigering = behandlinger.last().shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                korrigering.brukersMeldekort.id shouldBe korrigertMeldekort.id
                korrigering.harKorrigering shouldBe true

                brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            }
        }
    }

    @Test
    fun `skal behandle flere korrigeringer av samme kjede automatisk så lenge utbetalingen ikke er utbetalt`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val alleDagerDeltatt = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                ).dager
                val (førsteGlemteDag, andreGlemteDag) = alleDagerDeltatt.filter { it.status == InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET }

                fun dagerMedGlemt(vararg glemteDager: BrukersMeldekortDag) =
                    alleDagerDeltatt.map { if (it in glemteDager) it.copy(status = InnmeldtStatus.IKKE_TILTAKSDAG) else it }

                // Bruker glemte å melde to dager i førstegangsinnsendingen, og korrigerer dem til deltatt én om gangen.
                listOf(
                    dagerMedGlemt(førsteGlemteDag, andreGlemteDag),
                    dagerMedGlemt(andreGlemteDag),
                    alleDagerDeltatt,
                ).forEach { dager ->
                    val brukersMeldekort = ObjectMother.brukersMeldekort(
                        behandlesAutomatisk = true,
                        sakId = sak.id,
                        meldeperiode = meldeperiode,
                        mottatt = nå(clock),
                        dager = dager,
                    )
                    brukersMeldekortRepo.lagre(brukersMeldekort)
                    service.behandleBrukersMeldekort(clock)

                    brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                        .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
                }

                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 3
            }
        }
    }

    /**
     * Første meldekort behandles tirsdag 1. april 2025, så fristen for automatisk korrigering er onsdag 2. april kl 20:50.
     * Den første korrigeringen behandles onsdag, men flytter ikke fristen, så den andre korrigeringen torsdag er for sen.
     * Fristen for prioritering er den samme, så den andre korrigeringen prioriteres heller ikke.
     */
    @Test
    fun `skal regne fristen for automatisk korrigering og prioritering fra første behandling av kjeden`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val alleDagerDeltatt = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                ).dager
                val (førsteGlemteDag, andreGlemteDag) = alleDagerDeltatt.filter { it.status == InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET }

                fun dagerMedGlemt(vararg glemteDager: BrukersMeldekortDag) =
                    alleDagerDeltatt.map { if (it in glemteDager) it.copy(status = InnmeldtStatus.IKKE_TILTAKSDAG) else it }

                val (første, førsteKorrigering, andreKorrigering) = listOf(
                    dagerMedGlemt(førsteGlemteDag, andreGlemteDag) to 1.april(2025).atTime(12, 0),
                    dagerMedGlemt(andreGlemteDag) to 2.april(2025).atTime(10, 0),
                    alleDagerDeltatt to 3.april(2025).atTime(10, 0),
                ).map { (dager, tidspunkt) ->
                    val brukersMeldekort = ObjectMother.brukersMeldekort(
                        behandlesAutomatisk = true,
                        sakId = sak.id,
                        meldeperiode = meldeperiode,
                        mottatt = tidspunkt.minusHours(1),
                        dager = dager,
                    )
                    brukersMeldekortRepo.lagre(brukersMeldekort)
                    service.behandleBrukersMeldekort(fixedClockAt(tidspunkt))
                    brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                }

                første.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
                førsteKorrigering.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
                andreKorrigering.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
                andreKorrigering.prioritering shouldBe null
            }
        }
    }

    @Test
    fun `skal ikke behandle korrigering automatisk når simuleringen viser at meldeperioden er utbetalt`() {
        korrigerDagSomErMedIFørsteUtbetaling(utbetaltAvOppdrag = true).let {
            it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.HAR_TIDLIGERE_UTBETALING
            it.behandlesAutomatisk shouldBe false
            // Utbetalingen har allerede gått gjennom, så det er ingenting å rekke.
            it.prioritering shouldBe null
        }
    }

    @Test
    fun `skal behandle korrigering automatisk når simuleringen viser at meldeperioden ikke er utbetalt ennå`() {
        korrigerDagSomErMedIFørsteUtbetaling(utbetaltAvOppdrag = false)
            .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
    }

    /**
     * Behandler første meldekort automatisk og mottar så en korrigering som fjerner en dag fra utbetalingen.
     * Simuleringsfaken viser dagen som tidligere utbetalt, med mindre den simulerer mot saken slik den var før første meldekort.
     */
    private fun korrigerDagSomErMedIFørsteUtbetaling(utbetaltAvOppdrag: Boolean): BrukersMeldekort {
        var lagret: BrukersMeldekort? = null
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val førsteMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(førsteMeldekort)
                service.behandleBrukersMeldekort(clock)
                if (!utbetaltAvOppdrag) {
                    tac.utbetalingFakeKlient.sakUtbetaltAvOppdrag = sak
                }

                // Bruker meldte en dag som deltatt ved en feil, og korrigerer den.
                val korrigertMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = nå(clock),
                    dager = førsteMeldekort.dager.let { dager ->
                        val feilmeldtDag = dager.first { it.status == InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET }
                        dager.map { if (it == feilmeldtDag) it.copy(status = InnmeldtStatus.IKKE_TILTAKSDAG) else it }
                    },
                )
                brukersMeldekortRepo.lagre(korrigertMeldekort)
                service.behandleBrukersMeldekort(clock)
                lagret = brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort.id)!!
            }
        }
        return lagret!!
    }

    /** Bare korrigeringer stoppes av en tidligere utbetaling, siden det er korrigeringen som kan endre en utbetaling oppdragssystemet allerede har utbetalt. */
    @Test
    fun `skal behandle førstegangsinnsending automatisk selv om simuleringen viser tidligere utbetaling`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                val simulering = ObjectMother.simulering(
                    periode = meldeperiode.periode,
                    meldeperiode = meldeperiode,
                    simuleringsdager = nonEmptyListOf(
                        Simuleringsdag(
                            dato = meldeperiode.periode.fraOgMed,
                            tidligereUtbetalt = 298,
                            nyUtbetaling = 298,
                            totalEtterbetaling = 0,
                            totalFeilutbetaling = 0,
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
                        ),
                    ),
                )
                simulering.harTidligereUtbetaling shouldBe true
                tac.utbetalingFakeKlient.simulering = ObjectMother.simuleringMedMetadata(simulering)

                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(clock)

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            }
        }
    }

    @Test
    fun `skal ikke behandle korrigert meldekort automatisk uten simulering`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()

                val førsteMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(førsteMeldekort)
                service.behandleBrukersMeldekort(clock)

                val korrigertMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = meldeperiode,
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(korrigertMeldekort)

                tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.UkjentFeil(
                    httpKlientUventetStatus(statusCode = 500, body = "simulering feilet"),
                )
                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 1

                brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.SIMULERING_FEILET
                    // Statusen kan prøves på nytt, så korrigeringen behandles når simuleringen virker igjen.
                    it.behandlesAutomatisk shouldBe true
                }
            }
        }
    }

    @Test
    fun `skal ikke behandle korrigert meldekort automatisk når første behandling var manuell`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val kjede = sak.meldeperiodeKjeder.first()
                tac.meldekortbehandlingIverksatt(
                    sakId = sak.id,
                    kjedeId = kjede.kjedeId,
                )

                val korrigertMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(korrigertMeldekort)
                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 1

                brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET_MANUELT
                    it.behandlesAutomatisk shouldBe false
                }

                // Fristen regnes også fra en manuell behandling, og sjekkes før om tidligere behandlinger var manuelle.
                val forSenKorrigering = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = 3.april(2025).atTime(9, 0),
                )
                brukersMeldekortRepo.lagre(forSenKorrigering)
                service.behandleBrukersMeldekort(fixedClockAt(3.april(2025).atTime(10, 0)))

                brukersMeldekortRepo.hentForMeldekortId(forSenKorrigering.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
            }
        }
    }

    @Test
    fun `skal ikke behandle korrigert meldekort automatisk når en senere behandling av kjeden var manuell`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val kjede = sak.meldeperiodeKjeder.first()

                val førsteMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(førsteMeldekort)
                service.behandleBrukersMeldekort(clock)
                brukersMeldekortRepo.hentForMeldekortId(førsteMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET

                // Saksbehandler korrigerer meldeperioden manuelt etter den automatiske behandlingen.
                tac.meldekortbehandlingIverksatt(
                    sakId = sak.id,
                    kjedeId = kjede.kjedeId,
                )

                val korrigertMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                // Nyere innsending sjekkes før om tidligere behandlinger var manuelle.
                val eldreKorrigering = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = førsteMeldekort.mottatt.minusMinutes(1),
                )
                brukersMeldekortRepo.lagre(korrigertMeldekort)
                brukersMeldekortRepo.lagre(eldreKorrigering)
                // Jobben behandler ett meldekort per sak om gangen.
                service.behandleBrukersMeldekort(clock)
                service.behandleBrukersMeldekort(clock)

                brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.TIDLIGERE_BEHANDLET_MANUELT
                brukersMeldekortRepo.hentForMeldekortId(eldreKorrigering.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.NYERE_INNSENDING_ER_BEHANDLET
            }
        }
    }

    /**
     * Korrigeringen er mottatt før førstegangsinnsendingen som allerede er behandlet, så den ville overskrevet en nyere innsending.
     * Er de mottatt samtidig, vet vi ikke hvilken som er nyest, og korrigeringen behandles heller ikke automatisk.
     */
    @Test
    fun `skal ikke behandle korrigering automatisk når en nyere innsending er behandlet`() {
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 1.april(2025).atTime(12, 0),
            korrigeringMottatt = 1.april(2025).atTime(10, 0),
            Kjøring(1.april(2025).atTime(12, 5)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.NYERE_INNSENDING_ER_BEHANDLET)
        // Førstegangsinnsendingen mottas 1 time før den behandles.
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 1.april(2025).atTime(12, 0),
            korrigeringMottatt = 1.april(2025).atTime(11, 0),
            Kjøring(1.april(2025).atTime(12, 5)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.NYERE_INNSENDING_ER_BEHANDLET)
        // Fristen sjekkes før om en nyere innsending er behandlet.
        korrigerMeldekortAutomatisk(
            førsteBehandlingstidspunkt = 1.april(2025).atTime(12, 0),
            korrigeringMottatt = 1.april(2025).atTime(10, 0),
            Kjøring(3.april(2025).atTime(10, 0)),
        ) shouldBe listOf(MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE)
    }

    /** Revurderingen gir meldeperioden en ny versjon etter at førstegangsinnsendingen er behandlet, og korrigeringen sendes inn på den nye versjonen. */
    @Test
    fun `skal behandle korrigering automatisk selv om meldeperioden har fått ny versjon etter forrige behandling`() {
        // Hverdag innenfor økonomisystemets åpningstider, ellers hopper jobben over alle meldekort.
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0)))) { tac ->
            val (sak, andreVersjon, alleDagerDeltatt) = behandleFørsteMeldekortOgRevurderTilFlereDager(tac)

            val (_, korrigertMeldekort) = mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = andreVersjon.id,
                sakId = sak.id,
                dager = alleDagerDeltatt,
            )
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort!!.id)!!
                .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.let {
                it.size shouldBe 2
                it.last().shouldBeInstanceOf<MeldekortBehandletAutomatisk>().brukersMeldekort.meldeperiode shouldBe andreVersjon
            }
        }
    }

    /**
     * Første meldekort behandles fredag 2. mai 2025, så fristen for automatisk korrigering er mandag 5. mai kl 20:50.
     * Revurderingen gir meldeperioden flere dager, så den første korrigeringen på den nye versjonen behandles automatisk onsdag 7. mai, etter fristen og selv om meldeperioden er utbetalt.
     * Fristen for de neste korrigeringene av den nye versjonen regnes fra den behandlingen, og er torsdag 8. mai kl 20:50.
     * Korrigeringen torsdag behandles derfor automatisk, også den selv om meldeperioden er utbetalt, mens korrigeringen fredag er for sen og ikke prioriteres.
     */
    @Test
    fun `skal regne fristen for korrigering etter revurdering til flere dager fra første behandling av den nye versjonen`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0)))) { tac ->
            val (sak, andreVersjon, alleDagerDeltatt) = behandleFørsteMeldekortOgRevurderTilFlereDager(tac)
            tac.utbetalingFakeKlient.simulering = ObjectMother.simuleringMedMetadata(
                ObjectMother.simulering(
                    periode = andreVersjon.periode,
                    meldeperiode = andreVersjon,
                    simuleringsdager = nonEmptyListOf(
                        Simuleringsdag(
                            dato = andreVersjon.periode.fraOgMed,
                            tidligereUtbetalt = 298,
                            nyUtbetaling = 298,
                            totalEtterbetaling = 0,
                            totalFeilutbetaling = 0,
                            totalTrekk = 0,
                            totalJustering = 0,
                            totalMotpostering = 0,
                            harJustering = false,
                        ),
                    ),
                ),
            )

            val (førsteKorrigering, andreKorrigering, tredjeKorrigering) = listOf(
                7.mai(2025).atTime(12, 0),
                8.mai(2025).atTime(12, 0),
                9.mai(2025).atTime(12, 0),
            ).map { tidspunkt ->
                val (_, korrigering) = mottaMeldekortRequest(
                    tac = tac,
                    meldeperiodeId = andreVersjon.id,
                    sakId = sak.id,
                    dager = alleDagerDeltatt,
                    mottatt = tidspunkt.minusHours(1),
                )
                tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(fixedClockAt(tidspunkt))
                tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(korrigering!!.id)!!
            }

            førsteKorrigering.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            andreKorrigering.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            tredjeKorrigering.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_SENT_Å_KORRIGERE
            tredjeKorrigering.prioritering shouldBe null
            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 3
        }
    }

    /**
     * Korrigeringen fjerner en dag som er utbetalt, så simuleringen viser feilutbetaling selv om korrigeringen ellers kunne vært behandlet etter endret rett.
     * Simuleringen viser også at meldeperioden er utbetalt, så korrigeringen prioriteres ikke, selv om den er mottatt innen fristen regnet fra første behandling av kjeden.
     */
    @Test
    fun `skal ikke behandle korrigering etter revurdering til flere dager automatisk når simuleringen viser feilutbetaling`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0)))) { tac ->
            val (sak, andreVersjon, alleDagerDeltatt) = behandleFørsteMeldekortOgRevurderTilFlereDager(tac)
            val dagerDeltatt = alleDagerDeltatt.filterValues { it == BrukerutfyltMeldekortDTO.Status.DELTATT_UTEN_LØNN_I_TILTAKET }.keys
            val glemtDag = dagerDeltatt.first()
            val utbetaltDag = dagerDeltatt.last()

            val (_, korrigertMeldekort) = mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = andreVersjon.id,
                sakId = sak.id,
                dager = alleDagerDeltatt + listOf(glemtDag, utbetaltDag).associateWith { BrukerutfyltMeldekortDTO.Status.IKKE_TILTAKSDAG },
            )
            tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)

            tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(korrigertMeldekort!!.id)!!.let {
                it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING
                it.behandlesAutomatisk shouldBe false
                it.prioritering shouldBe null
            }
            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 1
        }
    }

    /**
     * Behandler en førstegangsinnsending der bruker glemte å melde en dag, og iverksetter så en revurdering som gir meldeperioden 11 dager i stedet for 10.
     * Klokka må stå på en hverdag innenfor økonomisystemets åpningstider, ellers hopper jobben over alle meldekort.
     *
     * @return saken, den nye versjonen av meldeperioden og dagene utfylt med deltatt på alle dager med rett.
     */
    private suspend fun ApplicationTestBuilder.behandleFørsteMeldekortOgRevurderTilFlereDager(
        tac: TestApplicationContext,
    ): Triple<Sak, Meldeperiode, Map<LocalDate, BrukerutfyltMeldekortDTO.Status>> {
        val deltakelse = tac.tiltaksdeltakelse(vedtaksperiode)
        val (sak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, deltakelse),
        )
        val førsteVersjon = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
        val alleDagerDeltatt = førsteVersjon.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort)
        val glemtDag = alleDagerDeltatt.entries.first { it.value == BrukerutfyltMeldekortDTO.Status.DELTATT_UTEN_LØNN_I_TILTAKET }.key

        val (_, førsteMeldekort) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = førsteVersjon.id,
            sakId = sak.id,
            dager = alleDagerDeltatt + (glemtDag to BrukerutfyltMeldekortDTO.Status.IKKE_TILTAKSDAG),
        )
        tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
        tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(førsteMeldekort!!.id)!!
            .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET

        val (sakEtterRevurdering) = iverksettRevurderingInnvilgelse(
            tac = tac,
            sakId = sak.id,
            innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, deltakelse, AntallDagerForMeldeperiode(11)),
        )
        val andreVersjon = sakEtterRevurdering.meldeperiodeKjeder.first().hentSisteMeldeperiode()
        andreVersjon.versjon shouldBe førsteVersjon.versjon.inc()

        return Triple(sakEtterRevurdering, andreVersjon, alleDagerDeltatt)
    }

    @Test
    fun `skal ikke behandle automatisk på andre kjede dersom første kjede med rett ikke er behandlet`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder[1].hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
                    it.behandlesAutomatisk shouldBe true
                }
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk på tredje kjede dersom bare første kjede er behandlet`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val førsteMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )
                brukersMeldekortRepo.lagre(førsteMeldekort)

                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 1

                val tredjeMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder[2].hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(tredjeMeldekort)

                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.size shouldBe 1

                brukersMeldekortRepo.hentForMeldekortId(tredjeMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_NESTE_KJEDE
                    it.behandlesAutomatisk shouldBe true
                }
            }
        }
    }

    @Test
    fun `skal behandle automatisk selv om saken har en åpen behandling på en annen kjede`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Saksbehandler har en åpen behandling på andre kjede.
                val (_, åpenBehandling) = tac.nyOpprettetMeldekortbehandling(
                    sakId = sak.id,
                    kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                service.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!
                meldekortbehandlinger.godkjenteMeldekort.size shouldBe 1
                val automatiskBehandling = meldekortbehandlinger.sisteGodkjenteMeldekort
                automatiskBehandling.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                automatiskBehandling.brukersMeldekort.id shouldBe brukersMeldekort.id

                // Den åpne behandlingen på andre kjede er urørt.
                meldekortbehandlinger.hentÅpenBehandlingForKjede(sak.meldeperiodeKjeder[1].kjedeId)!!.id shouldBe åpenBehandling.id

                brukersMeldekortRepo
                    .hentForMeldekortId(brukersMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk når saken har en åpen behandling på samme kjede`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val kjede = sak.meldeperiodeKjeder.first()

                // Saksbehandler har allerede tatt kjeden meldekortet gjelder.
                tac.nyOpprettetMeldekortbehandling(
                    sakId = sak.id,
                    kjedeId = kjede.kjedeId,
                )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                service.behandleBrukersMeldekort(clock)

                meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.shouldBeEmpty()

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING
                    // Statusen kan prøves på nytt, så meldekortet plukkes opp igjen når saksbehandler er ferdig.
                    it.behandlesAutomatisk shouldBe true
                }
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk et meldekort som var knyttet til en avbrutt behandling`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingJobb

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val kjede = sak.meldeperiodeKjeder.first()

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = kjede.hentSisteMeldeperiode(),
                    mottatt = nå(clock),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                // Saksbehandler tar kjeden før jobben har kjørt, og behandlingen knyttes til meldekortet.
                val (_, behandling) = tac.nyOpprettetMeldekortbehandling(
                    sakId = sak.id,
                    kjedeId = kjede.kjedeId,
                )
                service.behandleBrukersMeldekort(clock)
                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.HAR_ÅPEN_BEHANDLING

                tac.meldekortContext.avbrytMeldekortbehandlingService.avbryt(
                    AvbrytMeldekortbehandlingKommando(
                        sakId = sak.id,
                        meldekortId = behandling.id,
                        begrunnelse = "Behandles ikke".toNonBlankString(),
                        saksbehandler = ObjectMother.saksbehandler(),
                        correlationId = CorrelationId.generate(),
                    ),
                ).getOrFail()

                service.behandleBrukersMeldekort(clock = fixedClockAt(nå(clock).plusMinutes(2)))

                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.godkjenteMeldekort.shouldBeEmpty()
                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAv shouldBe behandling.id
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET_I_AVBRUTT_BEHANDLING
                }
            }
        }
    }

    @Test
    fun `skal behandle automatisk når saken har en åpen revurdering til forlengelse`() {
        // Hverdag innenfor økonomisystemets åpningstider, ellers hopper jobben over alle meldekort.
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0)))) { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac = tac)

            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            revurdering.erAvsluttet shouldBe false

            val brukersMeldekort = mottaMeldekortOgKjørJobb(tac, sak)

            val sisteMeldekortbehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.sisteGodkjenteMeldekort
            sisteMeldekortbehandling.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
            sisteMeldekortbehandling.brukersMeldekort.id shouldBe brukersMeldekort.id

            tac.meldekortContext.brukersMeldekortRepo
                .hentForMeldekortId(brukersMeldekort.id)!!
                .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
        }
    }

    @Test
    fun `skal ikke behandle automatisk når saken har en åpen revurdering til stans`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0)))) { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac = tac)

            revurdering.resultat.shouldBeInstanceOf<Revurderingsresultat.Stans>()

            val brukersMeldekort = mottaMeldekortOgKjørJobb(tac, sak)

            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING
                it.behandlesAutomatisk shouldBe true
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk når saken har en åpen omgjøring`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0)))) { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac = tac)!!

            // En omgjøring kan ende med opphør, selv om den starter som innvilgelse.
            revurdering.resultat.shouldBeInstanceOf<Omgjøringsresultat>()

            val brukersMeldekort = mottaMeldekortOgKjørJobb(tac, sak)

            tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.ER_UNDER_REVURDERING
                it.behandlesAutomatisk shouldBe true
            }
        }
    }

    private suspend fun ApplicationTestBuilder.mottaMeldekortOgKjørJobb(
        tac: TestApplicationContext,
        sak: Sak,
    ): BrukersMeldekort {
        val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
        val (_, brukersMeldekort) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
        )
        tac.meldekortContext.automatiskMeldekortbehandlingJobb.behandleBrukersMeldekort(tac.clock)
        return brukersMeldekort!!
    }
}
