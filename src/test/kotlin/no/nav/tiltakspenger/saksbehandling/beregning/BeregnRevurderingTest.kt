package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.nonEmptyListOf
import arrow.core.right
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.gyldigFnr
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.leggTilMeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyOpprettetRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySakMedVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterRevurderingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BeregnRevurderingTest {

    private val vedtaksperiodeSøknadsbehandling = Periode(1.januar(2025), 30.juni(2025))
    private val vedtaksperiodeRevurdering = vedtaksperiodeSøknadsbehandling.plusTilOgMed(14)

    private val sats2025 = Satser.sats(1.januar(2025))

    private fun sakMedRevurdering(
        antallBarnFraSøknad: Int = 0,
        periodeForSøknadsbehandling: Periode = vedtaksperiodeSøknadsbehandling,
        periodeForRevurdering: Periode = vedtaksperiodeRevurdering,
        tiltakskodeForRevurdering: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
    ): Pair<Sak, Revurdering> {
        val (sak) = nySakMedVedtak(
            vedtaksperiode = periodeForSøknadsbehandling,
            barnetillegg = if (antallBarnFraSøknad > 0) {
                barnetillegg(
                    periode = periodeForSøknadsbehandling,
                    antallBarn = AntallBarn(antallBarnFraSøknad),
                )
            } else {
                Barnetillegg.utenBarnetillegg(periodeForSøknadsbehandling)
            },
        ).first.genererMeldeperioder(fixedClock)

        val revurdering = nyOpprettetRevurderingInnvilgelse(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            saksopplysningsperiode = periodeForRevurdering,
            hentSaksopplysninger = {
                saksopplysninger(
                    fom = it.fraOgMed,
                    tom = it.tilOgMed,
                    tiltaksdeltakelse = listOf(
                        tiltaksdeltakelse(
                            typeKode = tiltakskodeForRevurdering,
                            fom = it.fraOgMed,
                            tom = it.tilOgMed,
                        ),
                    ),
                )
            },
        )

        return sak.leggTilRevurdering(revurdering) to revurdering
    }

    private fun oppdaterBehandlingKommando(
        revurdering: Revurdering,
        innvilgelsesperiode: Periode = vedtaksperiodeRevurdering,
        antallDagerPerMeldeperiode: Int = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperiode),
    ): OppdaterRevurderingKommando.Innvilgelse {
        return oppdaterRevurderingInnvilgelseKommando(
            sakId = revurdering.sakId,
            behandlingId = revurdering.id,
            begrunnelseVilkårsvurdering = "lol",
            innvilgelsesperioder = nonEmptyListOf(
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = innvilgelsesperiode,
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                ),
            ),
            barnetillegg = barnetillegg,
        )
    }

    @Test
    fun `Skal beregne etterbetaling for revurdering når en legger til barn`() {
        val (sak, revurdering) = sakMedRevurdering()

        val (sakMedMeldekortBehandlinger, meldekortBehandling) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val beløpFørRevurdering =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.beregnTotalBeløp()

        val kommando = oppdaterBehandlingKommando(
            revurdering = revurdering,
            barnetillegg = barnetillegg(
                periode = vedtaksperiodeRevurdering,
                antallBarn = AntallBarn(1),
            ),
        )

        val nyBeregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
        )

        nyBeregning.shouldNotBeNull()
        nyBeregning.size shouldBe 1

        // 8 dager med rett i første meldeperiode for dette vedtaket
        nyBeregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 8
        nyBeregning.totalBeløp shouldBe meldekortBehandling.beregning.totalBeløp + nyBeregning.barnetilleggBeløp

        nyBeregning.totalBeløp - beløpFørRevurdering shouldBe (sats2025.satsBarnetillegg * 8)
    }

    @Test
    fun `Skal beregne tilbakekreving for revurdering når en fjerner barn`() {
        val (sak, revurdering) = sakMedRevurdering(
            antallBarnFraSøknad = 2,
        )

        val (sakMedMeldekortBehandlinger, meldekortBehandling) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val beløpFørRevurdering =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.beregnTotalBeløp()

        val kommando = oppdaterBehandlingKommando(
            revurdering = revurdering,
            barnetillegg = barnetillegg(
                periode = vedtaksperiodeRevurdering,
                antallBarn = AntallBarn(1),
            ),
        )

        val nyBeregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
        )

        nyBeregning.shouldBeInstanceOf<Beregning>()
        nyBeregning.size shouldBe 1

        // 8 dager med rett i første meldeperiode for dette vedtaket
        nyBeregning.barnetilleggBeløp shouldBe sats2025.satsBarnetillegg * 8
        nyBeregning.totalBeløp shouldBe meldekortBehandling.beregning.totalBeløp - sats2025.satsBarnetillegg * 8

        nyBeregning.totalBeløp - beløpFørRevurdering shouldBe -(sats2025.satsBarnetillegg * 8)
    }

    @Test
    fun `Skal ikke beregne en revurdering dersom ingen tidligere beregninger`() {
        val (sak, revurdering) = sakMedRevurdering()

        val kommando = oppdaterBehandlingKommando(
            revurdering = revurdering,
        )

        sak.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
        ).shouldBeNull()
    }

    @Test
    fun `Skal returnere ny beregning selv om det ikke er endringer på tidligere beregninger`() {
        val (sak, revurdering) = sakMedRevurdering()

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = sak.meldeperiodeKjeder.first().periode,
        )

        val kommando = oppdaterBehandlingKommando(
            revurdering = revurdering,
        )

        sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
        )!!.size shouldBe 1
    }

    @Test
    fun `Skal ikke endre beregningen for dager før eller etter revurderingsperioden`() {
        // Revurderer første meldeperiode, bortsett fra første og siste dag
        val førsteMeldeperiode = Periode(30.desember(2024), 12.januar(2025))
        val revurderingsperiode = førsteMeldeperiode.plusFraOgMed(1).minusTilOgMed(1)

        val (sak, revurdering) = sakMedRevurdering(
            antallBarnFraSøknad = 1,
            periodeForRevurdering = revurderingsperiode,
        )

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = førsteMeldeperiode,
        )

        val førsteDagIMeldeperioden =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.first().dager.first()
        val sisteDagIMeldeperioden =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.last().dager.last()

        val kommando = oppdaterBehandlingKommando(
            revurdering = revurdering,
            innvilgelsesperiode = revurderingsperiode,
            barnetillegg = barnetillegg(
                periode = revurderingsperiode,
                antallBarn = AntallBarn(2),
            ),
        )

        val beregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
        )

        beregning.shouldNotBeNull()

        beregning.dager.find { it.dato == førsteDagIMeldeperioden.dato } shouldBe førsteDagIMeldeperioden
        beregning.dager.find { it.dato == sisteDagIMeldeperioden.dato } shouldBe sisteDagIMeldeperioden
    }

    @Test
    fun `Skal ha en sammenhengende beregningsperiode, selv om ikke alle meldeperioder har endringer`() {
        val (sak, revurdering) = sakMedRevurdering()

        val førstePeriode = sak.meldeperiodeKjeder[0].periode
        val andrePeriode = sak.meldeperiodeKjeder[1].periode
        val tredjePeriode = sak.meldeperiodeKjeder[2].periode

        val (sakMedMeldekortBehandlinger) = sak.leggTilMeldekortBehandletAutomatisk(
            periode = førstePeriode,
        ).let { (sak) ->
            sak.leggTilMeldekortBehandletAutomatisk(periode = andrePeriode)
        }.let { (sak) ->
            sak.leggTilMeldekortBehandletAutomatisk(periode = tredjePeriode)
        }

        val beløpFørRevurdering =
            sakMedMeldekortBehandlinger.meldeperiodeBeregninger.gjeldendeBeregninger.beregnTotalBeløp()

        val kommando = oppdaterBehandlingKommando(
            revurdering = revurdering,
            barnetillegg = barnetillegg(
                periodiseringAntallBarn = SammenhengendePeriodisering(
                    PeriodeMedVerdi(
                        AntallBarn(1),
                        førstePeriode,
                    ),
                    PeriodeMedVerdi(
                        AntallBarn(0),
                        andrePeriode,
                    ),
                    PeriodeMedVerdi(
                        AntallBarn(1),
                        tredjePeriode,
                    ),
                ),
            ),
        )

        val nyBeregning = sakMedMeldekortBehandlinger.beregnInnvilgelse(
            behandlingId = kommando.behandlingId,
            vedtaksperiode = kommando.innvilgelsesperioder.totalPeriode,
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
            barnetilleggsperioder = kommando.barnetillegg.periodisering,
        )

        // 8 dager med rett i første meldeperiode for dette vedtaket
        val forventetNyttBarnetillegg = sats2025.satsBarnetillegg * (8 + 10)

        nyBeregning.shouldNotBeNull()

        nyBeregning.dager.zipWithNext().all { (a, b) ->
            a.dato.plusDays(1) == b.dato
        }.shouldBeTrue()

        nyBeregning.size shouldBe 3

        nyBeregning.barnetilleggBeløp shouldBe forventetNyttBarnetillegg
        nyBeregning.totalBeløp shouldBe beløpFørRevurdering + forventetNyttBarnetillegg

        // Andre meldeperiode har ingen endringer på beregningen, 0 barn før og etter
        nyBeregning[1].dager shouldBe sakMedMeldekortBehandlinger.meldekortbehandlinger[1].beregning!!.dager
    }

    @Test
    fun `skal beregne ny utbetaling dersom en utbetalt periode opphøres og så innvilges på nytt`() {
        withTestApplicationContext { tac ->
            mockkStatic("no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ValiderKanIverksetteUtbetalingKt")

            every { any<Simulering>().validerKanIverksetteUtbetaling() } returns Unit.right()

            val periode = 1.januar(2025) til 31.januar(2025)

            val sak = tac.førsteMeldekortIverksatt(
                innvilgelsesperiode = periode,
                fnr = gyldigFnr(),
            )

            sak.meldeperiodeBeregninger.gjeldendeBeregninger.single().totalBeløp shouldBe 2384

            val (sakMedStans) = iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = periode.fraOgMed)

            sakMedStans.meldeperiodeBeregninger.gjeldendeBeregninger.single().totalBeløp shouldBe 0

            val (sakMedNyInnvilgelse) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(periode),
            )

            sakMedNyInnvilgelse.meldeperiodeBeregninger.gjeldendeBeregninger.single().totalBeløp shouldBe 2384
        }
    }
}
