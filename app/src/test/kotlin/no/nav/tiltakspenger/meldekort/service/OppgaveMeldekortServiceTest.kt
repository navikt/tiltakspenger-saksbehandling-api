package no.nav.tiltakspenger.meldekort.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.JournalpostIdGenerator
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.ports.SakRepo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveMeldekortServiceTest {

    private val oppgaveGateway = mockk<OppgaveGateway>(relaxed = true)
    private val sakRepo = mockk<SakRepo>()
    private val brukersMeldekortRepo = mockk<BrukersMeldekortRepo>()
    private val service = OppgaveMeldekortService(oppgaveGateway, sakRepo, brukersMeldekortRepo)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `skal opprette oppgave for gyldig meldekort`() {
        runTest {
            val journalpostId = JournalpostIdGenerator().neste()
            val meldekort = mockk<BrukersMeldekort>()
            every { meldekort.journalpostId } returns journalpostId
            every { meldekort.sakId } returns SakId.random()

            coEvery { brukersMeldekortRepo.hentMeldekortSomIKkeSkalGodkjennesAutomatisk() } returns listOf(meldekort)
            coEvery { sakRepo.hentForSakId(any()) } returns mockk(relaxed = true)

            service.opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk()

            coVerify {
                oppgaveGateway.opprettOppgave(
                    any(),
                    journalpostId,
                    Oppgavebehov.NYTT_MELDEKORT,
                )
            }
        }
    }

    @Test
    fun `ingen oppgave blir opprettet om journalpostId ikke finnes`() {
        runTest {
            val meldekort = mockk<BrukersMeldekort>()
            every { meldekort.journalpostId } returns null
            coEvery { brukersMeldekortRepo.hentMeldekortSomIKkeSkalGodkjennesAutomatisk() } returns listOf(meldekort)

            service.opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk()

            coVerify(exactly = 0) { oppgaveGateway.opprettOppgave(any(), any(), any()) }
        }
    }

    @Test
    fun `ingen oppgave opprettes om det ikke finnes en tilknyttet sak`() {
        runTest {
            val meldekort = mockk<BrukersMeldekort>()
            every { meldekort.journalpostId } returns JournalpostIdGenerator().neste()
            every { meldekort.sakId } returns SakId.random()
            coEvery { brukersMeldekortRepo.hentMeldekortSomIKkeSkalGodkjennesAutomatisk() } returns listOf(meldekort)
            coEvery { sakRepo.hentForSakId(any()) } returns null

            service.opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk()

            coVerify(exactly = 0) { oppgaveGateway.opprettOppgave(any(), any(), any()) }
        }
    }

    @Test
    fun `skal opprette oppgaver for gyldige meldekort`() {
        runTest {
            val meldekort = (1..20).map { mockk<BrukersMeldekort>(relaxed = true) }
            coEvery { brukersMeldekortRepo.hentMeldekortSomIKkeSkalGodkjennesAutomatisk() } returns meldekort
            coEvery { sakRepo.hentForSakId(any()) } returns mockk(relaxed = true)

            service.opprettOppgaveForMeldekortSomIkkeGodkjennesAutomatisk()

            coVerify(exactly = meldekort.size) {
                oppgaveGateway.opprettOppgave(any(), any(), Oppgavebehov.NYTT_MELDEKORT)
            }
        }
    }
}
