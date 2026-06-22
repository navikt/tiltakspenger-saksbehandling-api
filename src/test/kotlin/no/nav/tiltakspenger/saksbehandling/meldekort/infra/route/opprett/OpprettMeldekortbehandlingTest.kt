package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import org.junit.jupiter.api.Test

class OpprettMeldekortbehandlingTest {
    @Test
    fun `kan opprette meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 10.april(2025)),
            )
            val førsteMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first()
            val (oppdatertSak) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteMeldeperiode.kjedeId,
            )!!

            oppdatertSak.meldekortbehandlinger.single().meldeperioder.single().meldeperiode shouldBe førsteMeldeperiode
        }
    }

    @Test
    fun `kan opprette meldekortbehandling for meldeperiode som ikke gir rett`() {
        // 1. Iverksetter innvilget søknadsbehandling for jan 2025
        // 2. Iverksetter omgjøring som opphører alt bortsett fra 1. jan 2025
        withTestApplicationContext { tac ->
            val innvilgelsesperiodeSøknadsbehandling: Periode = 1 til 31.januar(2025)
            val innvilgelsesperiodeOmgjøring: Periode = 1 til 1.januar(2025)

            val (sak, _, rammevedtakSøknadsbehandling, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiodeSøknadsbehandling),
            )
            val (oppdatertSak) = this.iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtakSøknadsbehandling.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiodeOmgjøring),
            )
            oppdatertSak.meldeperiodeKjeder.size shouldBe 3
            oppdatertSak.meldeperiodeKjeder[0].last().antallDagerSomGirRett shouldBe 1
            oppdatertSak.meldeperiodeKjeder[1].last().antallDagerSomGirRett shouldBe 0
            oppdatertSak.meldeperiodeKjeder[2].last().antallDagerSomGirRett shouldBe 0

            val andreMeldeperiode = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede[1]

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreMeldeperiode.kjedeId,
            )
        }
    }

    @Test
    fun `kan ikke opprette meldekortbehandling dersom det allerede finnes en åpen behandling på kjeden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 10.april(2025)),
            )
            val kjedeId = sak.meldeperiodeKjeder.first().kjedeId

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
                forventetStatus = BadRequest,
                medJsonBody = { it harKode "HAR_ÅPEN_BEHANDLING" },
            )

            val sakEtter = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakEtter.meldekortbehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `kan ikke opprette meldekortbehandling på senere kjede før første kjede er behandlet`() {
        withTestApplicationContext { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1 til 31.januar(2025)),
            )
            sak.meldeperiodeKjeder.size shouldBe 3
            val andreKjedeId = sak.meldeperiodeKjeder[1].kjedeId

            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjedeId,
                forventetStatus = BadRequest,
                medJsonBody = { it harKode "MÅ_BEHANDLE_FØRSTE_KJEDE" },
            )

            val sakEtter = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakEtter.meldekortbehandlinger.shouldBeEmpty()
        }
    }

    @Test
    fun `kan opprette behandling for meldeperiode uten rett dersom det finnes et ubehandlet brukers-meldekort`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )
            val førsteKjede = sak.meldeperiodeKjeder.first()
            val førsteMeldeperiode = førsteKjede.hentSisteMeldeperiode()

            // Send brukers meldekort på første kjede mens den fortsatt gir rett
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = førsteMeldeperiode.id,
                sakId = sak.id,
                dager = førsteMeldeperiode.tilUtfyltFraBruker(),
            )

            // Omgjør slik at kun andre kjede beholder rett (første kjede mister all rett)
            val andreKjedePeriode = sak.meldeperiodeKjeder[1].periode
            this.iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(andreKjedePeriode),
            )

            val (oppdatertSak, meldekortbehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
            )!!

            meldekortbehandling.kjedeIdLegacy shouldBe førsteKjede.kjedeId
            meldekortbehandling.meldeperiodeLegacy.ingenDagerGirRett shouldBe true
            oppdatertSak.meldekortbehandlinger.single() shouldBe meldekortbehandling
        }
    }

    @Test
    fun `kan avbryte to påfølgende kjeder uten rett`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode: Periode = 1 til 31.januar(2025)
            val (sak, _, rammevedtak, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3
            val førsteKjede = sak.meldeperiodeKjeder[0]
            val andreKjede = sak.meldeperiodeKjeder[1]
            val førsteMeldeperiode = førsteKjede.hentSisteMeldeperiode()
            val andreMeldeperiode = andreKjede.hentSisteMeldeperiode()

            // 1. Brukers meldekort på begge kjeder mens de fortsatt gir rett, og omgjøring fjerner all rett fra de to første kjedene
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = førsteMeldeperiode.id,
                sakId = sak.id,
                dager = førsteMeldeperiode.tilUtfyltFraBruker(),
            )
            mottaMeldekortRequest(
                tac = tac,
                meldeperiodeId = andreMeldeperiode.id,
                sakId = sak.id,
                dager = andreMeldeperiode.tilUtfyltFraBruker(),
            )
            this.iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(27 til 31.januar(2025)),
            )

            // 2. Opprett behandling på første kjede og avbryt den
            val (_, førsteBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede.kjedeId,
            )!!
            val (sakEtterAvbryt, avbruttBehandling, _) = avbrytMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
            )!!
            avbruttBehandling.erAvbrutt shouldBe true
            sakEtterAvbryt.meldekortbehandlinger.single() shouldBe avbruttBehandling

            // 3. Nå skal det være mulig å opprette behandling på andre kjede
            val (_, andreBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = andreKjede.kjedeId,
            )!!
            andreBehandling.kjedeIdLegacy shouldBe andreKjede.kjedeId
            andreBehandling.meldeperiodeLegacy.ingenDagerGirRett shouldBe true
        }
    }
}
