package no.nav.tiltakspenger.saksbehandling.service

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeLagreBrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class MottaBrukerutfyltMeldekortServiceTest {

    @Test
    fun `Kan lagre brukers meldekort med gyldig kommando`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val meldeperiode = ObjectMother.meldeperiode()
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                oppgaveId = null,
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT else InnmeldtStatus.IKKE_REGISTRERT,
                        dato = it.key,
                    )
                },
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando)

            val meldekort = meldekortRepo.hentForMeldekortId(meldekortId)

            meldekort.shouldNotBeNull()
        }
    }

    @Test
    fun `Skal ikke lagre samme meldekort på nytt`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val meldeperiode = ObjectMother.meldeperiode()
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                oppgaveId = null,
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT else InnmeldtStatus.IKKE_REGISTRERT,
                        dato = it.key,
                    )
                },
            )

            val lagreKommandoMedDiff = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = lagreKommando.mottatt.minusDays(1),
                journalpostId = JournalpostId("asdf"),
                oppgaveId = null,
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT else InnmeldtStatus.IKKE_REGISTRERT,
                        dato = it.key,
                    )
                },
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando).shouldBe(Unit.right())

            val meldekortFraFørsteKommando = meldekortRepo.hentForMeldekortId(meldekortId)

            service.mottaBrukerutfyltMeldekort(lagreKommando).shouldBe(KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff.left())
            service.mottaBrukerutfyltMeldekort(lagreKommandoMedDiff).shouldBe(KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff.left())

            meldekortRepo.hentForMeldekortId(meldekortId) shouldBe meldekortFraFørsteKommando
        }
    }
}
