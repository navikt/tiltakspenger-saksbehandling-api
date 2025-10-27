package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.Companion.MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class MottaBrukerutfyltMeldekortServiceTest {

    @Test
    fun `Kan lagre brukers meldekort med gyldig kommando`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak()
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id)
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
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

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak()
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id)
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
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
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                        dato = it.key,
                    )
                },
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando).shouldBe(Unit.right())

            val meldekortFraFørsteKommando = meldekortRepo.hentForMeldekortId(meldekortId)

            service.mottaBrukerutfyltMeldekort(lagreKommando)
                .shouldBe(KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff.left())
            service.mottaBrukerutfyltMeldekort(lagreKommandoMedDiff)
                .shouldBe(KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff.left())

            meldekortRepo.hentForMeldekortId(meldekortId) shouldBe meldekortFraFørsteKommando
        }
    }

    @Test
    fun `Skal ikke flagge korrigerte meldekort for automatisk behandling`() {
        with(TestApplicationContext()) {
            val mottaService = this.mottaBrukerutfyltMeldekortService

            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak()
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id)
            val meldekortId = MeldekortId.random()
            val korrigertMeldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                        dato = it.key,
                    )
                },
            )

            val lagreKommandoKorrigering = LagreBrukersMeldekortKommando(
                id = korrigertMeldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = lagreKommando.mottatt.plusDays(1),
                journalpostId = JournalpostId("asdf2"),
                dager = meldeperiode.girRett.entries.map {
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (it.value) InnmeldtStatus.DELTATT_MED_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                        dato = it.key,
                    )
                },
            )

            mottaService.mottaBrukerutfyltMeldekort(lagreKommando).shouldBe(Unit.right())

            mottaService.mottaBrukerutfyltMeldekort(lagreKommandoKorrigering).shouldBe(Unit.right())

            val førsteMeldekort = meldekortRepo.hentForMeldekortId(meldekortId)
            val korrigertMeldekort = meldekortRepo.hentForMeldekortId(korrigertMeldekortId)

            førsteMeldekort!!.behandlesAutomatisk shouldBe true
            korrigertMeldekort!!.behandlesAutomatisk shouldBe false
            korrigertMeldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.ALLEREDE_BEHANDLET
        }
    }

    @Test
    fun `Skal flagge meldekort for automatisk behandling ved melding på helgedager dersom saken tillater det`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak(kanSendeInnHelgForMeldekort = true)
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id, girRettIHelg = true)
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.mapIndexed { index, entry ->
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (index < 7) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                        dato = entry.key,
                    )
                },
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando)

            val meldekort = meldekortRepo.hentForMeldekortId(meldekortId)

            meldekort.shouldNotBeNull()
            meldekort.behandlesAutomatisk shouldBe true
            meldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING
        }
    }

    @Test
    fun `Skal ikke flagge meldekort for automatisk behandling ved melding på helgedager uten flagg på saken`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak(kanSendeInnHelgForMeldekort = false)
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id, girRettIHelg = true)
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.mapIndexed { index, entry ->
                    BrukersMeldekort.BrukersMeldekortDag(
                        status = if (index < 7) InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET else InnmeldtStatus.IKKE_BESVART,
                        dato = entry.key,
                    )
                },
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando)

            val meldekort = meldekortRepo.hentForMeldekortId(meldekortId)

            meldekort.shouldNotBeNull()
            meldekort.behandlesAutomatisk shouldBe false
            meldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.KAN_IKKE_MELDE_HELG
        }
    }

    @Test
    fun `Skal ikke flagge meldekort for automatisk behandling ved for mange sammenhengende dager godkjent fravær`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak(kanSendeInnHelgForMeldekort = true)
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id, girRettIHelg = true)
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.chunked(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER + 1)
                    .mapIndexed { index, dager ->
                        dager.map {
                            BrukersMeldekort.BrukersMeldekortDag(
                                status = if (index % 2 == 0) InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV else InnmeldtStatus.IKKE_BESVART,
                                dato = it.key,
                            )
                        }
                    }.flatten(),
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando)

            val meldekort = meldekortRepo.hentForMeldekortId(meldekortId)

            meldekort.shouldNotBeNull()
            meldekort.behandlesAutomatisk shouldBe false
            meldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.FOR_MANGE_DAGER_GODKJENT_FRAVÆR
        }
    }

    @Test
    fun `Skal flagge meldekort for automatisk behandling ved akseptabelt antall sammenhengende dager godkjent fravær`() {
        with(TestApplicationContext()) {
            val service = this.mottaBrukerutfyltMeldekortService
            val meldeperiodeRepo = this.meldekortContext.meldeperiodeRepo
            val meldekortRepo = this.meldekortContext.brukersMeldekortRepo

            val sakRepo = this.sakContext.sakRepo
            val (sak) = nySakMedVedtak(kanSendeInnHelgForMeldekort = true)
            sakRepo.opprettSak(sak)

            val meldeperiode = ObjectMother.meldeperiode(sakId = sak.id, girRettIHelg = true)
            val meldekortId = MeldekortId.random()

            meldeperiodeRepo.lagre(meldeperiode)

            val lagreKommando = LagreBrukersMeldekortKommando(
                id = meldekortId,
                meldeperiodeId = meldeperiode.id,
                sakId = meldeperiode.sakId,
                mottatt = LocalDateTime.now(),
                journalpostId = JournalpostId("asdf"),
                dager = meldeperiode.girRett.entries.chunked(MAKS_SAMMENHENGENDE_GODKJENT_FRAVÆR_DAGER)
                    .mapIndexed { index, dager ->
                        dager.map {
                            BrukersMeldekort.BrukersMeldekortDag(
                                status = if (index % 2 == 0) InnmeldtStatus.FRAVÆR_GODKJENT_AV_NAV else InnmeldtStatus.IKKE_BESVART,
                                dato = it.key,
                            )
                        }
                    }.flatten(),
            )

            service.mottaBrukerutfyltMeldekort(lagreKommando)

            val meldekort = meldekortRepo.hentForMeldekortId(meldekortId)

            meldekort.shouldNotBeNull()
            meldekort.behandlesAutomatisk shouldBe true
            meldekort.behandletAutomatiskStatus shouldBe MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING
        }
    }
}
