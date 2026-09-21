package no.nav.tiltakspenger.saksbehandling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class VedtakslisteUtbetalingsoversiktExTest {
    @Test
    fun `sak uten vedtak har ingen periode`() {
        val (sak) = ObjectMother.sakMedOpprettetBehandling()

        sak.vedtaksliste.periodeForUtbetalingsoversikt() shouldBe null
    }

    @Test
    fun `rammevedtak gir perioden`() {
        val periode = Periode(1.januar(2025), 28.februar(2025))
        val (sak) = ObjectMother.nySakMedVedtak(vedtaksperiode = periode)

        sak.vedtaksliste.periodeForUtbetalingsoversikt() shouldBe periode
    }

    @Test
    fun `ramme- og meldekortvedtak gir yttergrensene`() {
        val (sak) = ObjectMother.nySakMedVedtak(vedtaksperiode = Periode(1.februar(2025), 30.april(2025)))
        val meldekortvedtak = ObjectMother.meldekortvedtak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            periode = Periode(6.januar(2025), 19.januar(2025)),
        )
        val vedtaksliste = Vedtaksliste(
            rammevedtaksliste = sak.rammevedtaksliste,
            meldekortvedtaksliste = Meldekortvedtaksliste(meldekortvedtak),
            klagevedtaksliste = Klagevedtaksliste.empty(),
        )

        vedtaksliste.periodeForUtbetalingsoversikt() shouldBe Periode(6.januar(2025), 30.april(2025))
    }
}
