package no.nav.tiltakspenger.saksbehandling.beregning

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MeldeperiodeBeregningerVedtattTest {

    @Nested
    inner class HenterForrigeEllerSisteBeregning {
        val sakId = SakId.random()
        val saksnummer = Saksnummer.genererSaknummer(8.desember(2025), "2025")
        val fnr = Fnr.random()

        @Test
        fun `henter forrige`() {
            val v1 = ObjectMother.meldekortvedtak(
                sakId = sakId,
                fnr = fnr,
                meldekortBehandling = ObjectMother.meldekortBehandletManuelt(sakId = sakId, fnr = fnr),
            )
            val v2 = ObjectMother.meldekortvedtak(
                sakId = sakId,
                fnr = fnr,
                meldekortBehandling = ObjectMother.meldekortBehandletManuelt(sakId = sakId, fnr = fnr),
            )

            val vedtaksliste = Vedtaksliste(
                Rammevedtaksliste.empty(),
                Meldekortvedtaksliste(listOf(v1, v2)),
                Klagevedtaksliste.empty(),
            )

            val meldeperiodeBeregningerVedtatt = MeldeperiodeBeregningerVedtatt.fraVedtaksliste(vedtaksliste)

            val actual = meldeperiodeBeregningerVedtatt.hentForrigeBeregningEllerSiste(
                v2.beregning.single().id,
                v2.beregning.single().kjedeId,
            )
            actual shouldBe v1.beregning.single()
        }

        @Test
        fun `henter siste`() {
            val v1 = ObjectMother.meldekortvedtak(
                sakId = sakId,
                fnr = fnr,
                meldekortBehandling = ObjectMother.meldekortBehandletManuelt(sakId = sakId, fnr = fnr),
            )

            val behandling = ObjectMother.meldekortBehandletManuelt(sakId = sakId, fnr = fnr)

            val vedtaksliste = Vedtaksliste(
                Rammevedtaksliste.empty(),
                Meldekortvedtaksliste(listOf(v1)),
                Klagevedtaksliste.empty(),
            )

            val meldeperiodeBeregningerVedtatt = MeldeperiodeBeregningerVedtatt.fraVedtaksliste(vedtaksliste)

            val actual = meldeperiodeBeregningerVedtatt.hentForrigeBeregningEllerSiste(
                behandling.beregning.single().id,
                behandling.beregning.single().kjedeId,
            )
            actual shouldBe v1.beregning.single()
        }
    }
}
