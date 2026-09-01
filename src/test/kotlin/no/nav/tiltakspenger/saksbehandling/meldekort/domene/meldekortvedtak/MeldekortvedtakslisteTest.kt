package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak

import arrow.core.toNonEmptyListOrThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilUtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock

class MeldekortvedtakslisteTest {

    private val p1 = Periode(6.januar(2025), 19.januar(2025))
    private val p2 = Periode(20.januar(2025), 2.februar(2025))
    private val p3 = Periode(3.februar(2025), 16.februar(2025))
    private val p4 = Periode(17.februar(2025), 2.mars(2025))

    @Nested
    inner class MeldeperiodebehandlingTidslinje {

        @Test
        fun `uten overlapp beholdes alle meldeperiodebehandlingene fra hvert sitt vedtak`() {
            val clock = TikkendeKlokke()
            val sakId = SakId.random()
            val saksnummer = ObjectMother.nesteSaksnummer()
            val fnr = Fnr.random()

            val v1 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p1, p2))
            val v2 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p3, p4))

            val tidslinje = Meldekortvedtaksliste(listOf(v1, v2)).meldeperiodebehandlingTidslinje

            tidslinje.perioderMedVerdi.map { it.periode to it.verdi.meldekortbehandlingId } shouldBe listOf(
                p1 to v1.meldekortId,
                p2 to v1.meldekortId,
                p3 to v2.meldekortId,
                p4 to v2.meldekortId,
            )
        }

        @Test
        fun `overlapp på slutten av det eldste vedtaket - nyeste vedtak vinner den overlappende meldeperioden`() {
            val clock = TikkendeKlokke()
            val sakId = SakId.random()
            val saksnummer = ObjectMother.nesteSaksnummer()
            val fnr = Fnr.random()

            val v1 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p1, p2))
            val v2 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p2, p3))

            val tidslinje = Meldekortvedtaksliste(listOf(v1, v2)).meldeperiodebehandlingTidslinje

            tidslinje.perioderMedVerdi.map { it.periode to it.verdi.meldekortbehandlingId } shouldBe listOf(
                p1 to v1.meldekortId,
                p2 to v2.meldekortId,
                p3 to v2.meldekortId,
            )
        }

        @Test
        fun `overlapp på starten av det eldste vedtaket - nyeste vedtak vinner den overlappende meldeperioden`() {
            val clock = TikkendeKlokke()
            val sakId = SakId.random()
            val saksnummer = ObjectMother.nesteSaksnummer()
            val fnr = Fnr.random()

            val v1 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p2, p3))
            val v2 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p1, p2))

            val tidslinje = Meldekortvedtaksliste(listOf(v1, v2)).meldeperiodebehandlingTidslinje

            tidslinje.perioderMedVerdi.map { it.periode to it.verdi.meldekortbehandlingId } shouldBe listOf(
                p1 to v2.meldekortId,
                p2 to v2.meldekortId,
                p3 to v1.meldekortId,
            )
        }

        @Test
        fun `overlapp i midten av det eldste vedtaket - nyeste vedtak vinner den overlappende meldeperioden`() {
            val clock = TikkendeKlokke()
            val sakId = SakId.random()
            val saksnummer = ObjectMother.nesteSaksnummer()
            val fnr = Fnr.random()

            val v1 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p1, p2, p3))
            val v2 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p2))

            val tidslinje = Meldekortvedtaksliste(listOf(v1, v2)).meldeperiodebehandlingTidslinje

            tidslinje.perioderMedVerdi.map { it.periode to it.verdi.meldekortbehandlingId } shouldBe listOf(
                p1 to v1.meldekortId,
                p2 to v2.meldekortId,
                p3 to v1.meldekortId,
            )
        }

        @Test
        fun `kjede av overlappende vedtak - nyeste vedtak vinner hver overlappende meldeperiode`() {
            val clock = TikkendeKlokke()
            val sakId = SakId.random()
            val saksnummer = ObjectMother.nesteSaksnummer()
            val fnr = Fnr.random()

            val v1 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p1, p2))
            val v2 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p2, p3))
            val v3 = meldekortvedtakMedMeldeperioder(clock, sakId, saksnummer, fnr, listOf(p3, p4))

            val tidslinje = Meldekortvedtaksliste(listOf(v1, v2, v3)).meldeperiodebehandlingTidslinje

            tidslinje.perioderMedVerdi.map { it.periode to it.verdi.meldekortbehandlingId } shouldBe listOf(
                p1 to v1.meldekortId,
                p2 to v2.meldekortId,
                p3 to v3.meldekortId,
                p4 to v3.meldekortId,
            )
        }
    }

    /**
     * Lager et meldekortvedtak der behandlingen dekker flere meldeperioder.
     * Periodene må være sortert og uten overlapp.
     */
    private fun meldekortvedtakMedMeldeperioder(
        clock: Clock,
        sakId: SakId,
        saksnummer: Saksnummer,
        fnr: Fnr,
        perioder: List<Periode>,
    ): Meldekortvedtak {
        val meldekortId = MeldekortId.random()

        val meldeperiodebehandlinger = perioder.map { periode ->
            Meldeperiodebehandling(
                dager = ObjectMother.meldeperiode(
                    periode = periode,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                ).tilUtfyltMeldeperiode(),
                brukersMeldekort = emptyList(),
                type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                meldekortbehandlingId = meldekortId,
            )
        }

        val beregning = Beregning(
            beregninger = perioder.map { periode ->
                ObjectMother.meldekortBeregning(
                    meldekortId = meldekortId,
                    startDato = periode.fraOgMed,
                    clock = clock,
                ).beregninger.single()
            }.toNonEmptyListOrThrow(),
            beregningstidspunkt = nå(clock),
        )

        val behandling = ObjectMother.meldekortBehandletManuelt(
            clock = clock,
            id = meldekortId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            periode = perioder.first(),
            meldekortperiodeBeregning = beregning,
        ).copy(
            meldeperioder = Meldeperiodebehandlinger(
                meldeperioder = meldeperiodebehandlinger.toNonEmptyListOrThrow(),
                beregning = beregning,
            ),
        )

        return ObjectMother.meldekortvedtak(
            clock = clock,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            meldekortbehandling = behandling,
        )
    }
}
