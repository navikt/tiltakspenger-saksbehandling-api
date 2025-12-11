package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.left
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.HAR_ÅPEN_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.INGEN_DAGER_GIR_RETT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_FØRSTE_KJEDE
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ValiderOpprettMeldekortbehandlingFeil.MÅ_BEHANDLE_NESTE_KJEDE
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandlingIverksatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.nyOpprettetMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingOmgjøring
import org.junit.jupiter.api.Test

class OpprettMeldekortBehandlingServiceTest {
    val førstePeriode = 6.januar(2025) til 19.januar(2025)
    val andrePeriode = førstePeriode.plus14Dager()
    val tredjePeriode = andrePeriode.plus14Dager()

    val allePeriodene = førstePeriode.fraOgMed til tredjePeriode.tilOgMed

    @Test
    fun `Skal kunne opprette behandling på første kjede når den gir rett`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac,
                vedtaksperiode = allePeriodene,
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
                tac,
                vedtaksperiode = allePeriodene,
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
                tac,
                vedtaksperiode = allePeriodene,
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
                tac,
                vedtaksperiode = allePeriodene,
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
                tac,
                vedtaksperiode = allePeriodene,
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
                søknadsbehandlingInnvilgelsesperiode = allePeriodene,
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
                tac,
                søknadsbehandlingInnvilgelsesperiode = allePeriodene,
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
    fun `Skal ikke opprette behandling på noen kjede dersom det finnes en åpen behandling`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandling(
                tac,
                vedtaksperiode = allePeriodene,
            )

            tac.nyOpprettetMeldekortbehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
            )

            tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ) shouldBe ValiderOpprettFeil(HAR_ÅPEN_BEHANDLING).left()

            tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
                kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
                sakId = sak.id,
                saksbehandler = saksbehandler(),
            ) shouldBe ValiderOpprettFeil(HAR_ÅPEN_BEHANDLING).left()
        }
    }
}
