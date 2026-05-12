package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksett

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test

class IverksettMeldekortbehandlingRouteTest {
    @Test
    fun `saksbehandler kan iverksette meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgMeldekortbehandling(
                tac = tac,
            )!!
            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.GODKJENT
        }
    }

    @Test
    fun `saksbehandler kan iverksette meldekortbehandling som spenner over to meldeperioder`() {
        val clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // 30.mars(2026) er en mandag, 26.april(2026) er en søndag => 2 meldeperioder (14 + 14 dager)
            // Vedtaksperiode 1.april (onsdag) - 26.april (søndag).
            // 2 barn => barnetillegg 56 kr per barn per deltatt dag.
            //
            // Kjede 1 (30.mars - 12.april): 30. og 31.mars uten rett. Resten med rett.
            //   Ukedager med rett: 1, 2, 3.april (3) + 6, 7, 8, 9, 10.april (5) = 8 dager
            //   => 8 dager DELTATT (under maks på 10)
            // Kjede 2 (13.april - 26.april): alle 14 dager med rett.
            //   Ukedager: 13-17.april (5) + 20-24.april (5) = 10 dager
            //   => 10 dager DELTATT (treffer maks på 10)
            //
            // Sats for 2026: 312 kr ordinær + 56 kr barnetillegg per barn per dag.
            val vedtaksperiode = 1.april(2026) til 26.april(2026)
            val antallBarn = 2
            val sats2026 = 312
            val satsBarnetillegg2026 = 56
            val forventetOrdinærKjede1 = 8 * sats2026 // 2496
            val forventetOrdinærKjede2 = 10 * sats2026 // 3120
            val forventetBarnetilleggKjede1 = 8 * satsBarnetillegg2026 * antallBarn // 896
            val forventetBarnetilleggKjede2 = 10 * satsBarnetillegg2026 * antallBarn // 1120
            val forventetTotalKjede1 = forventetOrdinærKjede1 + forventetBarnetilleggKjede1 // 3392
            val forventetTotalKjede2 = forventetOrdinærKjede2 + forventetBarnetilleggKjede2 // 4240
            val forventetOrdinærTotal = forventetOrdinærKjede1 + forventetOrdinærKjede2 // 5616
            val forventetBarnetilleggTotal = forventetBarnetilleggKjede1 + forventetBarnetilleggKjede2 // 2016
            val forventetTotalBeløp = forventetOrdinærTotal + forventetBarnetilleggTotal // 7632

            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
                barnetillegg = barnetillegg(periode = vedtaksperiode, antallBarn = AntallBarn(antallBarn)),
            )
            sak.meldeperiodeKjeder.size shouldBe 2

            val toMeldeperioder = sak.meldeperiodeKjeder.take(2).map { kjede ->
                kjede.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
            }

            val (_, meldekortvedtak, _) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = toMeldeperioder,
            )!!

            meldekortvedtak.meldekortbehandling.status shouldBe MeldekortbehandlingStatus.GODKJENT
            meldekortvedtak.periode shouldBe (30.mars(2026) til 26.april(2026))
            meldekortvedtak.beregningsperiode shouldBe (30.mars(2026) til 26.april(2026))
            meldekortvedtak.meldeperiodebehandlinger.meldeperioder.size shouldBe 2

            // Totalbeløp på utbetalingen
            meldekortvedtak.utbetaling.ordinærBeløp shouldBe forventetOrdinærTotal
            meldekortvedtak.utbetaling.barnetilleggBeløp shouldBe forventetBarnetilleggTotal
            meldekortvedtak.utbetaling.totalBeløp shouldBe forventetTotalBeløp

            // Aggregert beregning
            meldekortvedtak.beregning.ordinærBeløp shouldBe forventetOrdinærTotal
            meldekortvedtak.beregning.barnetilleggBeløp shouldBe forventetBarnetilleggTotal
            meldekortvedtak.beregning.totalBeløp shouldBe forventetTotalBeløp

            // Per meldeperiode
            meldekortvedtak.beregning.beregninger.size shouldBe 2
            meldekortvedtak.beregning.beregninger[0].ordinærBeløp shouldBe forventetOrdinærKjede1
            meldekortvedtak.beregning.beregninger[0].barnetilleggBeløp shouldBe forventetBarnetilleggKjede1
            meldekortvedtak.beregning.beregninger[0].totalBeløp shouldBe forventetTotalKjede1
            meldekortvedtak.beregning.beregninger[1].ordinærBeløp shouldBe forventetOrdinærKjede2
            meldekortvedtak.beregning.beregninger[1].barnetilleggBeløp shouldBe forventetBarnetilleggKjede2
            meldekortvedtak.beregning.beregninger[1].totalBeløp shouldBe forventetTotalKjede2
        }
    }

    @Test
    fun `iverksett meldekortbehandling over to meldeperioder med satsskifte over nyttår`() {
        val clock = TikkendeKlokke(fixedClockAt(1.mai(2026)))

        withTestApplicationContext(clock = clock) { tac ->
            // Vedtaksperiode: 22.desember 2025 (mandag) – 18.januar 2026 (søndag) => 2 meldeperioder
            // 2 barn => barnetillegg per dag per barn: 55 kr (2025), 56 kr (2026)
            //
            // Kjede 1 (22.des – 4.jan): alle 14 dager har rett.
            //   Ukedager 2025: 22, 23, 24, 25, 26, 29, 30, 31 des = 8 dager à 298 kr + 55*2 BT
            //   Ukedager 2026: 1, 2 jan = 2 dager à 312 kr + 56*2 BT
            //   => 10 dager DELTATT
            //   Ordinær:     8*298 + 2*312 = 2384 + 624 = 3008
            //   Barnetillegg: 8*55*2 + 2*56*2 = 880 + 224 = 1104
            //   Total kjede 1: 3008 + 1104 = 4112
            //
            // Kjede 2 (5.jan – 18.jan): alle 14 dager har rett (alle i 2026).
            //   Ukedager: 5, 6, 7, 8, 9, 12, 13, 14, 15, 16 jan = 10 dager à 312 kr + 56*2 BT
            //   Ordinær:     10*312 = 3120
            //   Barnetillegg: 10*56*2 = 1120
            //   Total kjede 2: 3120 + 1120 = 4240
            //
            // Totalt ordinær: 3008 + 3120 = 6128
            // Totalt barnetillegg: 1104 + 1120 = 2224
            // Totalt: 6128 + 2224 = 8352
            val antallBarn = 2
            val sats2025 = 298
            val sats2026 = 312
            val satsBarnetillegg2025 = 55
            val satsBarnetillegg2026 = 56
            val forventetOrdinærKjede1 = 8 * sats2025 + 2 * sats2026 // 3008
            val forventetOrdinærKjede2 = 10 * sats2026 // 3120
            val forventetBarnetilleggKjede1 = 8 * satsBarnetillegg2025 * antallBarn + 2 * satsBarnetillegg2026 * antallBarn // 1104
            val forventetBarnetilleggKjede2 = 10 * satsBarnetillegg2026 * antallBarn // 1120
            val forventetTotalKjede1 = forventetOrdinærKjede1 + forventetBarnetilleggKjede1 // 4112
            val forventetTotalKjede2 = forventetOrdinærKjede2 + forventetBarnetilleggKjede2 // 4240
            val forventetOrdinærTotal = forventetOrdinærKjede1 + forventetOrdinærKjede2 // 6128
            val forventetBarnetilleggTotal = forventetBarnetilleggKjede1 + forventetBarnetilleggKjede2 // 2224
            val forventetTotalBeløp = forventetOrdinærTotal + forventetBarnetilleggTotal // 8352

            val vedtaksperiode = 22.desember(2025) til 18.januar(2026)
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
                barnetillegg = barnetillegg(periode = vedtaksperiode, antallBarn = AntallBarn(antallBarn)),
            )
            sak.meldeperiodeKjeder.size shouldBe 2

            val toMeldeperioder = sak.meldeperiodeKjeder.take(2).map { kjede ->
                val mp = kjede.hentSisteMeldeperiode()
                val statuser = (0..13).map { offset ->
                    val dato = mp.periode.fraOgMed.plusDays(offset.toLong())
                    if (dato.erHelg()) {
                        MeldekortDagStatusDTO.IKKE_TILTAKSDAG
                    } else {
                        MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                    }
                }
                mp.tilOppdatertMeldeperiodeDTO(statuser)
            }

            val (_, meldekortvedtak, _) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
                meldeperioder = toMeldeperioder,
            )!!

            meldekortvedtak.meldekortbehandling.status shouldBe MeldekortbehandlingStatus.GODKJENT
            meldekortvedtak.periode shouldBe (22.desember(2025) til 18.januar(2026))
            meldekortvedtak.meldeperiodebehandlinger.meldeperioder.size shouldBe 2

            // Totalbeløp på utbetalingen
            meldekortvedtak.utbetaling.ordinærBeløp shouldBe forventetOrdinærTotal
            meldekortvedtak.utbetaling.barnetilleggBeløp shouldBe forventetBarnetilleggTotal
            meldekortvedtak.utbetaling.totalBeløp shouldBe forventetTotalBeløp

            // Aggregert beregning
            meldekortvedtak.beregning.ordinærBeløp shouldBe forventetOrdinærTotal
            meldekortvedtak.beregning.barnetilleggBeløp shouldBe forventetBarnetilleggTotal
            meldekortvedtak.beregning.totalBeløp shouldBe forventetTotalBeløp

            // Per meldeperiode
            meldekortvedtak.beregning.beregninger.size shouldBe 2
            meldekortvedtak.beregning.beregninger[0].ordinærBeløp shouldBe forventetOrdinærKjede1
            meldekortvedtak.beregning.beregninger[0].barnetilleggBeløp shouldBe forventetBarnetilleggKjede1
            meldekortvedtak.beregning.beregninger[0].totalBeløp shouldBe forventetTotalKjede1
            meldekortvedtak.beregning.beregninger[1].ordinærBeløp shouldBe forventetOrdinærKjede2
            meldekortvedtak.beregning.beregninger[1].barnetilleggBeløp shouldBe forventetBarnetilleggKjede2
            meldekortvedtak.beregning.beregninger[1].totalBeløp shouldBe forventetTotalKjede2
        }
    }
}
