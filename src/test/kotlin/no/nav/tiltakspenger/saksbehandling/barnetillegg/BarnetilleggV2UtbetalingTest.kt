package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggForBarnISøknaden
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggMedIdent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggV2
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tester at barna som er valgt i [BarnetilleggV2] gir riktig utbetaling, fra rammebehandlingen og helt fram til beregningen av meldekortene.
 *
 * Innvilgelsesperioden er 1. til 26. april 2026, som gir to meldeperioder: 30. mars til 12. april og 13. til 26. april.
 * Meldekortene har 8 og 10 deltatte dager: 1.–3. og 6.–10. april, og 13.–17. og 20.–24. april.
 * Satsen for 2026 er 312 kr i ordinær stønad og 56 kr i barnetillegg per barn per dag.
 *
 * Barna:
 * - Ola, lagt til av saksbehandler, gir barnetillegg i hele perioden.
 * - Kari, fra søknaden, fyller 16 år 15. april og gir barnetillegg til og med 14. april.
 * - Per, fra søknaden, blir født 8. april og gir barnetillegg fra og med fødselsdagen.
 * - Nils, lagt til av saksbehandler, har valgte perioder 1.–2. april og 20.–22. april.
 */
class BarnetilleggV2UtbetalingTest {

    private val innvilgelsesperiode = 1.april(2026) til 26.april(2026)
    private val sats = 312
    private val satsBarnetillegg = 56

    private val ola = barnMedBarnetillegg(fornavn = "Ola", fødselsdato = 1.januar(2020))
    private val nils = barnMedBarnetillegg(
        fornavn = "Nils",
        fødselsdato = 1.januar(2022),
        perioder = listOf(1.april(2026) til 2.april(2026), 20.april(2026) til 22.april(2026)),
    )
    private val kariISøknaden = barnetilleggMedIdent(fornavn = "Kari", mellomnavn = null, fødselsdato = 15.april(2010))
    private val perISøknaden = barnetilleggMedIdent(fornavn = "Per", mellomnavn = null, fødselsdato = 8.april(2026))

    @Test
    fun `barna som er valgt i søknadsbehandlingen gir riktig barnetillegg på meldekortet`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))) { tac ->
            val (sak) = iverksettSøknadsbehandlingMedBarn(tac)

            val beregning = iverksettMeldekortForBeggeMeldeperiodene(tac, sak)

            // Meldeperiode 1: Ola 8 + Kari 8 + Per 3 (8.–10. april) + Nils 2 (1.–2. april) = 21 barnedager.
            // Meldeperiode 2: Ola 10 + Kari 2 (13.–14. april) + Per 10 + Nils 3 (20.–22. april) = 25 barnedager.
            beregning.beregninger.map { it.ordinærBeløp } shouldBe listOf(8 * sats, 10 * sats)
            beregning.beregninger.map { it.barnetilleggBeløp } shouldBe listOf(21 * satsBarnetillegg, 25 * satsBarnetillegg)
            beregning.barnetilleggBeløp shouldBe 46 * satsBarnetillegg
            beregning.totalBeløp shouldBe 18 * sats + 46 * satsBarnetillegg

            beregning.antallBarnPerDag() shouldBe mapOf(
                1.april(2026) to 3, 2.april(2026) to 3, 3.april(2026) to 2,
                6.april(2026) to 2, 7.april(2026) to 2, 8.april(2026) to 3, 9.april(2026) to 3, 10.april(2026) to 3,
                13.april(2026) to 3, 14.april(2026) to 3, 15.april(2026) to 2, 16.april(2026) to 2, 17.april(2026) to 2,
                20.april(2026) to 3, 21.april(2026) to 3, 22.april(2026) to 3, 23.april(2026) to 2, 24.april(2026) to 2,
            )
        }
    }

    @Test
    fun `revurdering som endrer periodene for et barn beregner barnetillegget på nytt for meldekortene som er beregnet`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))) { tac ->
            val (sak, søknad, deltakelse) = iverksettSøknadsbehandlingMedBarn(tac)
            iverksettMeldekortForBeggeMeldeperiodene(tac, sak)

            // Nils får barnetillegg i hele perioden i stedet for de valgte periodene.
            // Revurderingen kan bare øke utbetalingen, siden feilutbetaling krever omgjøring.
            val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, deltakelse),
                barnetilleggRevurdering = barnetilleggV2(
                    innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
                    barn = listOf(ola, søknad.barn("Kari"), søknad.barn("Per"), nils.copy(perioder = null)),
                ),
            )

            // Meldeperiode 1: Ola 8 + Kari 8 + Per 3 + Nils 8 = 27 barnedager.
            // Meldeperiode 2: Ola 10 + Kari 2 + Per 10 + Nils 10 = 32 barnedager.
            val beregning = revurderingsvedtak.beregning.shouldNotBeNull()
            beregning.beregninger.map { it.ordinærBeløp } shouldBe listOf(8 * sats, 10 * sats)
            beregning.beregninger.map { it.barnetilleggBeløp } shouldBe listOf(27 * satsBarnetillegg, 32 * satsBarnetillegg)
            beregning.totalBeløp shouldBe 18 * sats + 59 * satsBarnetillegg
            beregning.antallBarnPerDag().getValue(3.april(2026)) shouldBe 3
            beregning.antallBarnPerDag().getValue(8.april(2026)) shouldBe 4
            beregning.antallBarnPerDag().getValue(15.april(2026)) shouldBe 3
            beregning.antallBarnPerDag().getValue(24.april(2026)) shouldBe 3
        }
    }

    @Test
    fun `omgjøring som fjerner et barn gir lavere barnetillegg for meldekortene som er beregnet`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(12.mai(2026)))) { tac ->
            val (sak, søknad, deltakelse, søknadsvedtak) = iverksettSøknadsbehandlingMedBarn(tac)
            iverksettMeldekortForBeggeMeldeperiodene(tac, sak)

            val (_, omgjøringsvedtak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, deltakelse),
                barnetilleggRevurdering = barnetilleggV2(
                    innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
                    barn = listOf(ola, søknad.barn("Per"), nils),
                ),
            )

            // Kari er fjernet.
            // Meldeperiode 1: Ola 8 + Per 3 + Nils 2 = 13 barnedager.
            // Meldeperiode 2: Ola 10 + Per 10 + Nils 3 = 23 barnedager.
            val beregning = omgjøringsvedtak.beregning.shouldNotBeNull()
            beregning.beregninger.map { it.ordinærBeløp } shouldBe listOf(8 * sats, 10 * sats)
            beregning.beregninger.map { it.barnetilleggBeløp } shouldBe listOf(13 * satsBarnetillegg, 23 * satsBarnetillegg)
            beregning.totalBeløp shouldBe 18 * sats + 36 * satsBarnetillegg
            beregning.antallBarnPerDag().getValue(1.april(2026)) shouldBe 2
            beregning.antallBarnPerDag().getValue(14.april(2026)) shouldBe 2
        }
    }

    private data class Søknadsvedtak(val sak: Sak, val søknad: Søknad, val deltakelse: TiltaksdeltakelseIntern, val vedtak: Rammevedtak)

    private suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingMedBarn(tac: TestApplicationContext): Søknadsvedtak {
        val deltakelse = tac.tiltaksdeltakelse(innvilgelsesperiode)
        val (sak, søknad, vedtak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, deltakelse),
            barnFraSøknad = listOf(kariISøknaden, perISøknaden),
            barnetilleggForSøknad = barnetilleggForBarnISøknaden(
                innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
                andreBarn = listOf(ola, nils),
            ),
        )
        sak.meldeperiodeKjeder.size shouldBe 2
        return Søknadsvedtak(sak, søknad, deltakelse, vedtak)
    }

    private suspend fun ApplicationTestBuilder.iverksettMeldekortForBeggeMeldeperiodene(tac: TestApplicationContext, sak: Sak): Beregning {
        val (_, meldekortvedtak) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            meldeperioder = sak.meldeperiodeKjeder.map { it.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO() },
        )!!
        meldekortvedtak.utbetaling.barnetilleggBeløp shouldBe meldekortvedtak.beregning.barnetilleggBeløp
        return meldekortvedtak.beregning
    }

    private fun Søknad.barn(fornavn: String): BarnMedBarnetillegg = barnetillegg.single { it.fornavn == fornavn }.tilBarnMedBarnetillegg(id)

    /** Antall barn per dag med barnetillegg. */
    private fun Beregning.antallBarnPerDag(): Map<LocalDate, Int> = beregninger.flatMap { it.dager }
        .filter { it.beløpBarnetillegg > 0 }
        .associate { it.dato to it.beregningsdag!!.antallBarn.value }
}
