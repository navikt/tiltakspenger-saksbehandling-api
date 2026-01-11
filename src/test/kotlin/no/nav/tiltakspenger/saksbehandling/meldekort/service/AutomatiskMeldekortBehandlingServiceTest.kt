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
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.søknadsbehandlingIverksattMedMeldeperioder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AutomatiskMeldekortBehandlingServiceTest {
    val clock = TikkendeKlokke(fixedClockAt(1.april(2025).atTime(12, 0)))
    val vedtaksperiode = Periode(6.januar(2025), 31.mars(2025))

    @Test
    fun `skal behandle brukers meldekort automatisk ved behandlesAutomatisk=true`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService
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

                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(clock)

                val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)!!
                val sisteMeldekortBehandling = meldekortBehandlinger.sisteGodkjenteMeldekort!!

                sisteMeldekortBehandling.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                sisteMeldekortBehandling.brukersMeldekort.id shouldBe brukersMeldekort.id
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk ved behandlesAutomatisk=false`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService

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

                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(clock)

                val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)

                meldekortBehandlinger shouldBe null
            }
        }
    }

    @Test
    fun `skal ikke behandle automatisk med for mange dager registrert`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->

                val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortBehandlingService =
                    tac.meldekortContext.automatiskMeldekortBehandlingService
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
                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(clock)

                val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)

                meldekortBehandlinger shouldBe null
            }
        }
    }

    @Test
    fun `skal kun behandle det neste meldekortet på en sak for hvert kall`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService
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

                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(clock)

                val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)!!

                meldekortBehandlinger.godkjenteMeldekort.size shouldBe 1
                meldekortBehandlinger.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                meldekortBehandlinger.sisteGodkjenteMeldekort!!.brukersMeldekort!!.id shouldBe brukersMeldekort1.id

                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(
                    clock.plus(
                        1,
                        ChronoUnit.MINUTES,
                    ),
                )

                val meldekortBehandlingerNeste = meldekortBehandlingRepo.hentForSakId(sak.id)!!

                meldekortBehandlingerNeste.godkjenteMeldekort.size shouldBe 2
                meldekortBehandlingerNeste.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
                meldekortBehandlingerNeste.sisteGodkjenteMeldekort!!.brukersMeldekort!!.id shouldBe brukersMeldekort2.id
            }
        }
    }

    @Test
    fun `skal feile dersom det allerede finnes en behandling på meldeperiodekjeden`() {
        runTest {
            withTestApplicationContext(clock = clock) { tac ->
                val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortBehandlingService =
                    tac.meldekortContext.automatiskMeldekortBehandlingService
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
                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(clock)

                val brukersMeldekortDuplikat = ObjectMother.brukersMeldekort(
                    behandlesAutomatisk = true,
                    sakId = sak.id,
                    meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
                )

                brukersMeldekortRepo.lagre(brukersMeldekortDuplikat)

                automatiskMeldekortBehandlingService.behandleBrukersMeldekort(clock)

                val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)

                meldekortBehandlinger!!.godkjenteMeldekort.size shouldBe 1

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
                val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
                val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
                val automatiskMeldekortBehandlingService =
                    tac.meldekortContext.automatiskMeldekortBehandlingService
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

                    automatiskMeldekortBehandlingService.behandleBrukersMeldekort(
                        fixedClockAt(
                            dag.atTime(5, 59),
                        ),
                    )
                    automatiskMeldekortBehandlingService.behandleBrukersMeldekort(
                        fixedClockAt(
                            dag.atTime(21, 0),
                        ),
                    )
                    automatiskMeldekortBehandlingService.behandleBrukersMeldekort(
                        fixedClockAt(
                            dag.atTime(21, 1),
                        ),
                    )
                }

                for (dagOffset in DayOfWeek.SATURDAY.ordinal..DayOfWeek.SUNDAY.ordinal) {
                    val dag = mandag.plusDays(dagOffset.toLong())
                    automatiskMeldekortBehandlingService.behandleBrukersMeldekort(
                        fixedClockAt(
                            dag.atTime(12, 0),
                        ),
                    )
                }

                assertNull(
                    meldekortBehandlingRepo.hentForSakId(sak.id),
                    "Forventet ingen meldekortbehandlinger for bruker",
                )
            }
        }
    }
}
