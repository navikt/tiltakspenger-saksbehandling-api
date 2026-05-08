package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AutomatiskMeldekortbehandlingService.Companion.MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.søknadsbehandlingIverksattMedMeldeperioder
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AutomatiskMeldekortbehandlingServiceTest {
    val clock = TikkendeKlokke(fixedClockAt(1.april(2025).atTime(12, 0)))
    val vedtaksperiode = Periode(6.januar(2025), 31.mars(2025))

    @Test
    fun `skal behandle brukers meldekort automatisk ved behandlesAutomatisk=true`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingService = tac.meldekortContext.automatiskMeldekortbehandlingService
                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!
                val sisteMeldekortbehandling = meldekortbehandlinger.sisteGodkjenteMeldekort!!

                sisteMeldekortbehandling.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                sisteMeldekortbehandling.brukersMeldekortLegacy!!.id shouldBe brukersMeldekort.id
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk ved behandlesAutomatisk=false`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingService = tac.meldekortContext.automatiskMeldekortbehandlingService

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

                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock)

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
                val automatiskMeldekortbehandlingService =
                    tac.meldekortContext.automatiskMeldekortbehandlingService
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
                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock)

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
                val automatiskMeldekortbehandlingService = tac.meldekortContext.automatiskMeldekortbehandlingService
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

                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)!!

                meldekortbehandlinger.godkjenteMeldekort.size shouldBe 1
                meldekortbehandlinger.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                meldekortbehandlinger.sisteGodkjenteMeldekort!!.brukersMeldekortLegacy!!.id shouldBe brukersMeldekort1.id

                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(
                    clock.plus(
                        1,
                        ChronoUnit.MINUTES,
                    ),
                )

                val meldekortbehandlingerNeste = meldekortbehandlingRepo.hentForSakId(sak.id)!!

                meldekortbehandlingerNeste.godkjenteMeldekort.size shouldBe 2
                meldekortbehandlingerNeste.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                meldekortbehandlingerNeste.sisteGodkjenteMeldekort!!.brukersMeldekortLegacy!!.id shouldBe brukersMeldekort2.id
            }
        }
    }

    @Test
    fun `skal feile dersom det allerede finnes en behandling på meldeperiodekjeden`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingService =
                    tac.meldekortContext.automatiskMeldekortbehandlingService
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
                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock)

                val brukersMeldekortDuplikat = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )

                brukersMeldekortRepo.lagre(brukersMeldekortDuplikat)

                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock)

                val meldekortbehandlinger = meldekortbehandlingRepo.hentForSakId(sak.id)

                meldekortbehandlinger!!.godkjenteMeldekort.size shouldBe 1

                brukersMeldekortRepo
                    .hentForMeldekortId(brukersMeldekort.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.BEHANDLET
                brukersMeldekortRepo
                    .hentForMeldekortId(brukersMeldekortDuplikat.id)!!
                    .behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
            }
        }
    }

    @Test
    fun `skal ikke behandle brukers meldekort automatisk utenfor økonomisystemets åpningstider`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortbehandlingService =
                    tac.meldekortContext.automatiskMeldekortbehandlingService
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

                    automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock = klokken0559)
                    automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock = klokken2100)
                    automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock = klokken2101)
                }

                for (dagOffset in DayOfWeek.SATURDAY.ordinal..DayOfWeek.SUNDAY.ordinal) {
                    val dag = mandag.plusDays(dagOffset.toLong())
                    val klokken1200 = fixedClockAt(dag.atTime(12, 0))

                    automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock = klokken1200)
                }

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            }
        }
    }

    @Test
    fun `skal prøve på nytt etter 1 minutt for første forsøk`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingService

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val forrigeForsøk = LocalDateTime.now(clock)
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
                val service = tac.meldekortContext.automatiskMeldekortbehandlingService

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val forrigeForsøk = LocalDateTime.now(clock)
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
                val service = tac.meldekortContext.automatiskMeldekortbehandlingService

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val forrigeForsøk = LocalDateTime.now(clock)
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
    fun `skal ikke prøve på nytt ved status som ikke skal retries (feks ALLEREDE_BEHANDLET)`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val service = tac.meldekortContext.automatiskMeldekortbehandlingService

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)
                service.behandleBrukersMeldekort(clock)

                // Duplikat for samme meldeperiode -> vil feile med ALLEREDE_BEHANDLET
                val duplikat = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                )
                brukersMeldekortRepo.lagre(duplikat)
                service.behandleBrukersMeldekort(clock)

                val lagretDuplikat = brukersMeldekortRepo.hentForMeldekortId(duplikat.id)!!
                lagretDuplikat.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
                // Skal være markert som ikke-automatisk, slik at den ikke plukkes opp igjen
                lagretDuplikat.behandlesAutomatisk shouldBe false

                // Selv langt frem i tid (godt over maxDelay) skal den ikke retries
                service.behandleBrukersMeldekort(clock = fixedClockAt(LocalDateTime.now(clock).plusHours(1)))
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
                val automatiskMeldekortbehandlingService =
                    tac.meldekortContext.automatiskMeldekortbehandlingService
                val nå = LocalDateTime.now(clock)

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
                automatiskMeldekortbehandlingService.behandleBrukersMeldekort(clock = nesteDag)

                meldekortbehandlingRepo.hentForSakId(sak.id) shouldBe null
            }
        }
    }

    @Test
    fun `skal slutte å prøve på nytt etter MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val service = tac.meldekortContext.automatiskMeldekortbehandlingService
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo

                val sak = tac.søknadsbehandlingIverksattMedMeldeperioder(
                    periode = vedtaksperiode,
                    clock = clock,
                )

                // Bruker andre kjede slik at behandlingen faller tilbake på en retry-bar status (MÅ_BEHANDLE_FØRSTE_KJEDE)
                val mottatt = LocalDateTime.now(clock)
                val brukersMeldekort = ObjectMother.brukersMeldekort(
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder[1].hentSisteMeldeperiode(),
                    behandlesAutomatisk = true,
                    mottatt = mottatt,
                )
                brukersMeldekortRepo.lagre(brukersMeldekort)

                // Innenfor MAKS_DELAY (1 dag etter mottatt) -> skal forbli markert for automatisk retry
                service.behandleBrukersMeldekort(clock = fixedClockAt(mottatt.plusHours(23)))

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
                    it.behandlesAutomatisk shouldBe true
                }

                // Etter MAKS_DELAY (mer enn 1 dag siden mottatt) -> skal ikke prøves automatisk på nytt
                service.behandleBrukersMeldekort(clock = fixedClockAt(mottatt.plusSeconds(1) + MAKS_DELAY_FOR_AUTOMATISK_BEHANDLING))

                brukersMeldekortRepo.hentForMeldekortId(brukersMeldekort.id)!!.let {
                    it.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.MÅ_BEHANDLE_FØRSTE_KJEDE
                    it.behandlesAutomatisk shouldBe false
                }
            }
        }
    }
}
