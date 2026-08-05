package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgAvbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test

/**
 * Testene kjører med testklokka som står 1. mai 2025.
 */
class MeldeperiodeKjedeDTOTest {

    @Test
    fun `kjede uten åpen behandling som har startet og gir rett kan behandles`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            sak.tilMeldeperiodeKjederDTO(tac.clock).first().also {
                it.kanBehandles shouldBe true
                it.kanIkkeBehandlesGrunn shouldBe null
            }
        }
    }

    @Test
    fun `kjede med åpen behandling kan ikke behandles`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )
            val kjedeMedÅpenBehandling = sak.meldeperiodeKjeder.first().kjedeId

            val (oppdatertSak) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeMedÅpenBehandling,
            )!!

            oppdatertSak.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjedeMedÅpenBehandling.toString() }
                .also {
                    it.kanBehandles shouldBe false
                    it.kanIkkeBehandlesGrunn shouldBe KanIkkeBehandlesGrunnDTO.HAR_ÅPEN_BEHANDLING
                }
        }
    }

    @Test
    fun `kjede som ikke har startet kan ikke behandles`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            sak.tilMeldeperiodeKjederDTO(tac.clock).last().also {
                it.periode.fraOgMed shouldBe 26.mai(2025).toString()
                it.kanBehandles shouldBe false
                it.kanIkkeBehandlesGrunn shouldBe KanIkkeBehandlesGrunnDTO.MELDEPERIODEN_HAR_IKKE_STARTET
            }
        }
    }

    @Test
    fun `kjede uten dager som gir rett kan ikke behandles`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (oppdatertSak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(1 til 1.januar(2025)),
            )

            val kjeder = oppdatertSak.tilMeldeperiodeKjederDTO(tac.clock)

            kjeder[0].also {
                it.kanBehandles shouldBe true
                it.kanIkkeBehandlesGrunn shouldBe null
            }
            kjeder[1].also {
                it.kanBehandles shouldBe false
                it.kanIkkeBehandlesGrunn shouldBe KanIkkeBehandlesGrunnDTO.INGEN_DAGER_GIR_RETT
            }
        }
    }

    @Test
    fun `brukers meldekort med avbrutt behandling får status AVBRUTT`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            val kjede = sak.meldeperiodeKjeder.first()

            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0)

            val (sakMedAvbruttBehandling) = opprettOgAvbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
            )!!

            sakMedAvbruttBehandling.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjede.kjedeId.toString() }
                .brukersMeldekortStatus shouldBe BrukersMeldekortStatusDTO.AVBRUTT
        }
    }

    @Test
    fun `korrigering fra bruker med avbrutt behandling får status KORRIGERING_AVBRUTT`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            val kjede = sak.meldeperiodeKjeder.first()

            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0)
            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0, journalpostId = "4321")

            val (sakMedAvbruttBehandling) = opprettOgAvbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
            )!!

            sakMedAvbruttBehandling.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjede.kjedeId.toString() }
                .brukersMeldekortStatus shouldBe BrukersMeldekortStatusDTO.KORRIGERING_AVBRUTT
        }
    }

    @Test
    fun `brukers meldekort med åpen behandling får status UNDER_BEHANDLING`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            val kjede = sak.meldeperiodeKjeder.first()

            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0)

            val (sakMedÅpenBehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
            )!!

            sakMedÅpenBehandling.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjede.kjedeId.toString() }
                .brukersMeldekortStatus shouldBe BrukersMeldekortStatusDTO.UNDER_BEHANDLING
        }
    }

    @Test
    fun `korrigering fra bruker med åpen behandling får status KORRIGERING_UNDER_BEHANDLING`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            val kjede = sak.meldeperiodeKjeder.first()

            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0)
            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0, journalpostId = "4321")

            val (sakMedÅpenBehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
            )!!

            sakMedÅpenBehandling.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjede.kjedeId.toString() }
                .brukersMeldekortStatus shouldBe BrukersMeldekortStatusDTO.KORRIGERING_UNDER_BEHANDLING
        }
    }

    @Test
    fun `brukers meldekort mottatt etter at behandlingen ble åpnet venter fortsatt på behandling`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 31.mai(2025)),
            )

            val kjede = sak.meldeperiodeKjeder.first()

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
            )!!

            mottaMeldekortFraBruker(tac = tac, sak = sak, kjedeIndeks = 0)

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.tilMeldeperiodeKjederDTO(tac.clock)
                .single { it.id == kjede.kjedeId.toString() }
                .brukersMeldekortStatus shouldBe BrukersMeldekortStatusDTO.VENTER_BEHANDLING
        }
    }

    private suspend fun ApplicationTestBuilder.mottaMeldekortFraBruker(
        tac: TestApplicationContext,
        sak: Sak,
        kjedeIndeks: Int,
        journalpostId: String = "1234",
    ) {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = meldeperiode.tilUtfyltFraBruker(
                kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
            ),
            journalpostId = journalpostId,
        )
    }
}
