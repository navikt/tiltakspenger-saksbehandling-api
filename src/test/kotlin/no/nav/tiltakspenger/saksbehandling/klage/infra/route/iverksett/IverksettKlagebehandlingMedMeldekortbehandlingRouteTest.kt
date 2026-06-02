package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgIverksettKlagebehandlingOpprettholdelseMedMeldekortbehandling
import org.junit.jupiter.api.Test

class IverksettKlagebehandlingMedMeldekortbehandlingRouteTest {

    @Test
    fun `kan iverksette klagebehandling (opprettholdelse) med meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2026)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, meldekortvedtak, iverksattMeldekort, iverksattKlagebehandling) =
                iverksettSøknadsbehandlingOgIverksettKlagebehandlingOpprettholdelseMedMeldekortbehandling(
                    tac = tac,
                )!!

            iverksattKlagebehandling.status shouldBe Klagebehandlingsstatus.VEDTATT
            iverksattKlagebehandling.erVedtatt shouldBe true
            iverksattKlagebehandling.erAvsluttet shouldBe true
            iverksattKlagebehandling.erUnderBehandling shouldBe false
            iverksattKlagebehandling.iverksattTidspunkt shouldBe iverksattMeldekort.iverksattTidspunkt

            iverksattMeldekort.status shouldBe MeldekortbehandlingStatus.GODKJENT
            iverksattMeldekort.klagebehandling?.status shouldBe Klagebehandlingsstatus.VEDTATT
            iverksattMeldekort.klagebehandling?.id shouldBe iverksattKlagebehandling.id

            meldekortvedtak.behandlingId shouldBe iverksattMeldekort.id

            val sakFraRepo = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            val klagebehandlingFraRepo = sakFraRepo.hentKlagebehandling(iverksattKlagebehandling.id)
            klagebehandlingFraRepo.status shouldBe Klagebehandlingsstatus.VEDTATT
        }
    }
}
