package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.left
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandlingIverksatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.nyOpprettetMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingOmgjøring
import org.junit.jupiter.api.Test

class OpprettMeldekortBehandlingServiceTest {
    val førstePeriode = 6.januar(2025) til 19.januar(2025)
    val andrePeriode = førstePeriode.plus14Dager()
    val tredjePeriode = andrePeriode.plus14Dager()

    val totalPeriode = førstePeriode.fraOgMed til tredjePeriode.tilOgMed

    @Test
    fun `Skal kunne opprette behandling på første kjede når den gir rett`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            val (oppdatertSak, meldekortbehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ).getOrFail()

            oppdatertSak.meldekortbehandlinger.single() shouldBe meldekortbehandling
        }
    }

    @Test
    fun `Skal kunne opprette behandling på andre kjede når første kjede er ferdig behandlet`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            tac.meldekortbehandlingIverksatt(
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )

            val (oppdatertSak, meldekortbehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ).getOrFail()

            oppdatertSak.meldekortbehandlinger.size shouldBe 2
            oppdatertSak.meldekortbehandlinger.last() shouldBe meldekortbehandling
        }
    }

    @Test
    fun `Skal kunne opprette korrigering`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            tac.meldekortbehandlingIverksatt(
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )

            val (oppdatertSak, meldekortbehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                saksbehandler = saksbehandler(),
            ).getOrFail()

            meldekortbehandling.erKorrigering shouldBe true
            oppdatertSak.meldekortbehandlinger.size shouldBe 2
            oppdatertSak.meldekortbehandlinger.last() shouldBe meldekortbehandling
        }
    }

    @Test
    fun `Skal ikke kunne opprette behandling på andre kjede dersom første kjede med rett ikke er behandlet`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ) shouldBe ValiderOpprettFeil(MÅ_BEHANDLE_FØRSTE_KJEDE).left()
        }
    }

    @Test
    fun `Skal ikke kunne opprette behandling på tredje kjede dersom bare første kjede er behandlet`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            tac.meldekortbehandlingIverksatt(
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )

            tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder[2].kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ) shouldBe ValiderOpprettFeil(MÅ_BEHANDLE_NESTE_KJEDE).left()
        }
    }

    @Test
    fun `Skal kunne opprette behandling på andre kjede når første kjede ikke gir rett`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = totalPeriode,
                revurderingInnvilgelsesperiode = andrePeriode,
            )!!

            val (oppdatertSak, meldekortbehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ).getOrFail()

            oppdatertSak.meldekortbehandlinger.single() shouldBe meldekortbehandling
        }
    }

    @Test
    fun `Skal ikke opprette behandling for meldeperiode uten rett`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = totalPeriode,
                revurderingInnvilgelsesperiode = andrePeriode,
            )!!

            tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ) shouldBe ValiderOpprettFeil(INGEN_DAGER_GIR_RETT).left()
        }
    }

    @Test
    fun `Kan opprette behandling for meldeperiode uten rett dersom det finnes et ubehandlet brukers-meldekort`() {
        withTestApplicationContext { tac ->
            val (sak, _, vedtak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            val brukersMeldekortMedRett = ObjectMother.brukersMeldekort(
                sakId = sak.id,
                meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode(),
            )

            tac.meldekortContext.brukersMeldekortRepo.lagre(brukersMeldekortMedRett)

            iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                innvilgelsesperiode = andrePeriode,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = andrePeriode.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        tiltaksdeltakelseId = vedtak.behandling.saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
                    ),
                ),
            )

            val (_, meldekortbehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ).getOrFail()

            meldekortbehandling.kjedeId shouldBe sak.meldeperiodeKjeder.first().kjedeId
            meldekortbehandling.meldeperiode.ingenDagerGirRett shouldBe true
        }
    }

    @Test
    fun `Skal ikke opprette behandling på noen kjede dersom det finnes en åpen behandling`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            tac.nyOpprettetMeldekortbehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
            )

            sak.meldeperiodeKjeder.forEach {
                tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                    kjedeId = it.kjedeId,
                    sakId = sak.id,
                    saksbehandler = saksbehandler(),
                ) shouldBe ValiderOpprettFeil(HAR_ÅPEN_BEHANDLING).left()
            }
        }
    }

    @Test
    fun `Skal gjenopprette behandling som er lagt tilbake`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = totalPeriode,
            )

            val (_, nyBehandling) = tac.nyOpprettetMeldekortbehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
            )

            val (sakMedLagtTilbake, behandlingLagtTilbake) = tac.meldekortContext.leggTilbakeMeldekortBehandlingService.leggTilbakeMeldekortBehandling(
                sakId = sak.id,
                meldekortId = nyBehandling.id,
                saksbehandler = saksbehandler(navIdent = nyBehandling.saksbehandler!!),
            )

            sakMedLagtTilbake.meldekortbehandlinger.single() shouldBe behandlingLagtTilbake
            behandlingLagtTilbake.status shouldBe MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING

            val (sakMedGjenopprettet, gjenopprettetBehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ).getOrFail()

            sakMedGjenopprettet.meldekortbehandlinger.single() shouldBe gjenopprettetBehandling
            gjenopprettetBehandling.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING
        }
    }
}
