package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgOppdaterMeldekortbehandling
import org.junit.jupiter.api.Test

class OppdaterMeldekortbehandlingRouteTest {

    @Test
    fun `kan oppdatere meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                begrunnelse = "oppdatert begrunnelse",
            )!!

            meldekortbehandling.begrunnelse!!.verdi shouldBe "oppdatert begrunnelse"
        }
    }

    @Test
    fun `meldekortperioden kan ikke være frem i tid`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                vedtaksperiode = 1.januar(2030) til 31.januar(2030),
                forventetStatus = HttpStatusCode.BadRequest,
                medJsonBody = {
                    it harKode "meldekortperioden_kan_ikke_være_frem_i_tid"
                },
            )
        }
    }

    @Test
    fun `kan velge å ikke sende vedtaksbrev`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling) = this.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                skalSendeVedtaksbrev = false,
            )!!

            meldekortbehandling.skalSendeVedtaksbrev shouldBe false
            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)!!.skalSendeVedtaksbrev shouldBe false
        }
    }

    @Test
    fun `kan oppdatere meldekortbehandling som spenner over to meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // Starter på en onsdag
            val vedtaksperiode = 1.april(2026) til 30.april(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            val toMeldeperioder = sak.meldeperiodeKjeder.take(2).map {
                it.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            }

            val (oppdatertSak, oppdatertMeldekortbehandling) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = toMeldeperioder,
            )!!

            oppdatertMeldekortbehandling.meldeperioder.meldeperioder.size shouldBe 2
            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.beløpTotal!! shouldBeGreaterThan 0
            oppdatertMeldekortbehandling.periode shouldBe (30.mars(2026) til 26.april(2026))

            oppdatertSak.meldekortbehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `kan oppdatere meldekortbehandling som spenner over tre meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // 30.mars(2026) er en mandag, 10.mai(2026) er en søndag => 3 meldeperioder (14 + 14 + 14 dager)
            val vedtaksperiode = 1.april(2026) til 10.mai(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3

            val treMeldeperioder = sak.meldeperiodeKjeder.take(3).map {
                it.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            }

            val (oppdatertSak, oppdatertMeldekortbehandling) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = treMeldeperioder,
            )!!

            oppdatertMeldekortbehandling.meldeperioder.meldeperioder.size shouldBe 3
            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.beløpTotal!! shouldBeGreaterThan 0
            oppdatertMeldekortbehandling.periode shouldBe (30.mars(2026) til 10.mai(2026))

            oppdatertSak.meldekortbehandlinger.size shouldBe 1
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med ikke-sammenhengende meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            val vedtaksperiode = 1.april(2026) til 10.mai(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )
            sak.meldeperiodeKjeder.size shouldBe 3

            // Hopper over meldeperiode 2 (midten) - kjede 0 og kjede 2 er ikke sammenhengende
            val ikkeSammenhengendeMeldeperioder = listOf(
                sak.meldeperiodeKjeder[0].hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO(),
                sak.meldeperiodeKjeder[2].hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO(),
            )

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = ikkeSammenhengendeMeldeperioder,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med samme meldeperiodekjede flere ganger`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            val vedtaksperiode = 1.april(2026) til 30.april(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            val sammeKjede = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = listOf(sammeKjede, sammeKjede),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med utdatert versjon av meldeperiode`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // Søknadsbehandling dekker tirsdag 31.mars - søndag 12.april (1.april er onsdag)
            // Revurderingen forlenger bakover til mandag 30.mars, slik at 30.mars og 31.mars
            // går fra "ingen rett" til "har rett" og kjeden får en ny versjon.
            val (sak) = this.iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(1.april(2026) til 12.april(2026)),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(30.mars(2026) til 12.april(2026)),
            )

            val kjede = sak.meldeperiodeKjeder.first()
            kjede.size shouldBe 2 // versjon 1 (søknadsbehandling) og versjon 2 (revurdering)

            // Bruker den utdaterte (første) versjonen av meldeperioden. Den har et annet
            // girRett-mønster enn siste versjon, så valideringen i UtfyltMeldeperiode skal feile.
            val utdatertMeldeperiode = kjede.first().tilOppdatertMeldeperiodeDTO()

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjede.kjedeId,
                meldeperioder = listOf(utdatertMeldeperiode),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere meldekortbehandling med kjedeId som ikke finnes på saken`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            val vedtaksperiode = 1.april(2026) til 30.april(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            // Bygger en gyldig DTO basert på sakens første meldeperiode, men setter en kjedeId
            // som ikke finnes på saken.
            val gyldigDto = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            val ukjentKjede = gyldigDto.copy(kjedeId = "2099-01-05/2099-01-18")

            opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = listOf(ukjentKjede),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }
}
