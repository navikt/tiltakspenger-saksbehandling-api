package no.nav.tiltakspenger.saksbehandling.beregning

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.leggTilMeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingStans
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test

class BeregnRevurderingStansTest {
    private val sats2024 = Satser.sats(1.januar(2024))
    private val sats2025 = Satser.sats(1.januar(2025))

    // Starter på en tirsdag
    private val virkningsperiode = Periode(31.desember(2024), 30.juni(2025))

    private fun sakMedToMeldekortOgStans(): Pair<Sak, Revurdering> {
        val (sak) = nySakMedVedtak(
            virkningsperiode = virkningsperiode,
            barnetillegg =
            barnetillegg(
                periode = virkningsperiode,
                antallBarn = AntallBarn(1),
            ),
        ).first.genererMeldeperioder(fixedClock)

        val førsteMeldeperiode = sak.meldeperiodeKjeder.first().periode

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = førsteMeldeperiode,
        ).first.leggTilMeldekortBehandletAutomatisk(periode = førsteMeldeperiode.plus14Dager())

        val revurdering = nyOpprettetRevurderingStans(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            virkningsperiode = virkningsperiode,
        )

        return sakMedMeldekortBehandlinger.copy(
            behandlinger = sak.behandlinger.leggTilRevurdering(revurdering),
        ) to revurdering
    }

    @Test
    fun `Skal ikke beregne utbetaling ved stans over kun ikke-utbetalte perioder`() {
        val (sak, revurdering) = sakMedToMeldekortOgStans()

        val beregning = sak.beregnRevurderingStans(revurdering.id, 27.januar(2025))

        beregning.shouldBeNull()
    }

    @Test
    fun `Skal ikke endre utbetalingen ved stans kun over dager uten tidligere utbetaling`() {
        // Stanser lørdag/søndag på andre meldekort
        val (sak, revurdering) = sakMedToMeldekortOgStans()

        val beregning = sak.beregnRevurderingStans(revurdering.id, 25.januar(2025))

        beregning.shouldNotBeNull()
        beregning.size shouldBe 1

        beregning.beregningKilde.id shouldBe revurdering.id
        beregning[0].meldekortId shouldBe sak.meldekortBehandlinger[1].id

        beregning.totalBeløp shouldBe sak.meldekortBehandlinger[1].beløpTotal
    }

    @Test
    fun `Skal beregne 0-utbetaling for hele virkningperioden når hele perioden stanses`() {
        val (sak, revurdering) = sakMedToMeldekortOgStans()

        val beregning = sak.beregnRevurderingStans(revurdering.id, virkningsperiode.fraOgMed)

        beregning.shouldNotBeNull()
        beregning.size shouldBe 2

        beregning.beregningKilde.id shouldBe revurdering.id
        beregning[0].meldekortId shouldBe sak.meldekortBehandlinger[0].id
        beregning[1].meldekortId shouldBe sak.meldekortBehandlinger[1].id

        beregning.ordinærBeløp shouldBe 0
        beregning.barnetilleggBeløp shouldBe 0
    }

    @Test
    fun `Skal beregne 0-utbetaling for en utbetalt meldeperiode når denne perioden stanses`() {
        val (sak, revurdering) = sakMedToMeldekortOgStans()

        val beregning = sak.beregnRevurderingStans(revurdering.id, 13.januar(2025))

        beregning.shouldNotBeNull()
        beregning.size shouldBe 1

        beregning.beregningKilde.id shouldBe revurdering.id
        beregning[0].meldekortId shouldBe sak.meldekortBehandlinger[1].id

        beregning.ordinærBeløp shouldBe 0
        beregning.barnetilleggBeløp shouldBe 0
    }

    @Test
    fun `Skal beregne redusert utbetaling ved stans midt i siste utbetalte periode`() {
        val (sak, revurdering) = sakMedToMeldekortOgStans()

        val beregning = sak.beregnRevurderingStans(revurdering.id, 20.januar(2025))

        beregning.shouldNotBeNull()
        beregning.size shouldBe 1

        beregning.beregningKilde.id shouldBe revurdering.id
        beregning[0].meldekortId shouldBe sak.meldekortBehandlinger[1].id

        beregning.ordinærBeløp shouldBe sats2025.sats * 5
        beregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 5
    }

    @Test
    fun `Skal beregne en redusert utbetaling og en 0-utbetaling ved stans midt i første utbetalte periode`() {
        val (sak, revurdering) = sakMedToMeldekortOgStans()

        val beregning = sak.beregnRevurderingStans(revurdering.id, 6.januar(2025))

        beregning.shouldNotBeNull()
        beregning.size shouldBe 2

        beregning.beregningKilde.id shouldBe revurdering.id
        beregning[0].meldekortId shouldBe sak.meldekortBehandlinger[0].id
        beregning[1].meldekortId shouldBe sak.meldekortBehandlinger[1].id

        beregning.ordinærBeløp shouldBe (sats2024.sats * 1) + (sats2025.sats * 3)
        beregning.barnetilleggBeløp shouldBe (sats2024.satsBarnetillegg * 1) + (sats2025.satsBarnetillegg * 3)

        beregning[0].totalBeløp shouldBe beregning.totalBeløp
        beregning[1].totalBeløp shouldBe 0
    }
}
