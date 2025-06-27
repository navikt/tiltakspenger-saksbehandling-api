package no.nav.tiltakspenger.saksbehandling.meldekort.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.søknadsbehandlingIverksattMedMeldeperioder
import org.junit.jupiter.api.Test

class AutomatiskMeldekortBehandlingServiceTest {
    val clock = fixedClockAt(1.april(2025))
    val virkningsperiode = Periode(6.januar(2025), 31.mars(2025))

    @Test
    fun `skal behandle brukers meldekort automatisk ved behandlesAutomatisk=true`() {
        val tac = TestApplicationContext()
        val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
        val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService

        val sak = runBlocking {
            tac.søknadsbehandlingIverksattMedMeldeperioder(
                periode = virkningsperiode,
                clock = clock,
            )
        }

        val brukersMeldekort = ObjectMother.brukersMeldekort(
            behandlesAutomatisk = true,
            sakId = sak.id,
            meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
        )

        brukersMeldekortRepo.lagre(brukersMeldekort)

        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)!!
        val sisteMeldekortBehandling = meldekortBehandlinger.sisteGodkjenteMeldekort!!

        sisteMeldekortBehandling.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
        sisteMeldekortBehandling.brukersMeldekort.id shouldBe brukersMeldekort.id
    }

    @Test
    fun `skal ikke behandle automatisk ved behandlesAutomatisk=false`() {
        val tac = TestApplicationContext()
        val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
        val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService

        val sak = runBlocking {
            tac.søknadsbehandlingIverksattMedMeldeperioder(
                periode = virkningsperiode,
                clock = clock,
            )
        }

        val brukersMeldekort = ObjectMother.brukersMeldekort(
            behandlesAutomatisk = false,
            sakId = sak.id,
            meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
        )

        brukersMeldekortRepo.lagre(brukersMeldekort)

        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)

        meldekortBehandlinger shouldBe null
    }

    @Test
    fun `skal ikke behandle automatisk med for mange dager registrert`() {
        val tac = TestApplicationContext()
        val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
        val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService

        val sak = runBlocking {
            tac.søknadsbehandlingIverksattMedMeldeperioder(
                periode = virkningsperiode,
                clock = clock,
                antallDagerPerMeldeperiode = SammenhengendePeriodisering(AntallDagerForMeldeperiode(10), virkningsperiode),
            )
        }

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

        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)

        meldekortBehandlinger shouldBe null
    }

    @Test
    fun `skal kun behandle det neste meldekortet på en sak for hvert kall`() {
        val tac = TestApplicationContext()
        val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
        val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService

        val sak = runBlocking {
            tac.søknadsbehandlingIverksattMedMeldeperioder(
                periode = virkningsperiode,
                clock = clock,
            )
        }

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

        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)!!

        meldekortBehandlinger.godkjenteMeldekort.size shouldBe 1
        meldekortBehandlinger.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
        meldekortBehandlinger.sisteGodkjenteMeldekort!!.brukersMeldekort!!.id shouldBe brukersMeldekort1.id

        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val meldekortBehandlingerNeste = meldekortBehandlingRepo.hentForSakId(sak.id)!!

        meldekortBehandlingerNeste.godkjenteMeldekort.size shouldBe 2
        meldekortBehandlingerNeste.sisteGodkjenteMeldekort.shouldBeInstanceOf<MeldekortBehandletAutomatisk>()
        meldekortBehandlingerNeste.sisteGodkjenteMeldekort!!.brukersMeldekort!!.id shouldBe brukersMeldekort2.id
    }

    @Test
    fun `skal feile dersom det allerede finnes en behandling på meldeperiodekjeden`() {
        val tac = TestApplicationContext()
        val meldekortBehandlingRepo = tac.meldekortContext.meldekortBehandlingRepo
        val brukersMeldekortRepo = tac.meldekortContext.brukersMeldekortRepo
        val automatiskMeldekortBehandlingService = tac.meldekortContext.automatiskMeldekortBehandlingService

        val sak = runBlocking {
            tac.søknadsbehandlingIverksattMedMeldeperioder(
                periode = virkningsperiode,
                clock = clock,
            )
        }

        val brukersMeldekort = ObjectMother.brukersMeldekort(
            behandlesAutomatisk = true,
            sakId = sak.id,
            meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
        )

        brukersMeldekortRepo.lagre(brukersMeldekort)
        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val brukersMeldekortDuplikat = ObjectMother.brukersMeldekort(
            behandlesAutomatisk = true,
            sakId = sak.id,
            meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
        )

        brukersMeldekortRepo.lagre(brukersMeldekortDuplikat)
        runBlocking {
            automatiskMeldekortBehandlingService.behandleBrukersMeldekort()
        }

        val meldekortBehandlinger = meldekortBehandlingRepo.hentForSakId(sak.id)

        meldekortBehandlinger!!.godkjenteMeldekort.size shouldBe 1

        brukersMeldekortRepo
            .hentForMeldekortId(brukersMeldekort.id)!!
            .behandletAutomatiskStatus shouldBe BrukersMeldekortBehandletAutomatiskStatus.BEHANDLET
        brukersMeldekortRepo
            .hentForMeldekortId(brukersMeldekortDuplikat.id)!!
            .behandletAutomatiskStatus shouldBe BrukersMeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
    }
}
