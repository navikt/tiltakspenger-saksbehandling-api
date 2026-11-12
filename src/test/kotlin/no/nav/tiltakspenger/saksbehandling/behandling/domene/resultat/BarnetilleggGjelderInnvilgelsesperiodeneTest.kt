package no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV1
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import org.junit.jupiter.api.Test

class BarnetilleggGjelderInnvilgelsesperiodeneTest {

    private val januar = 1 til 31.januar(2026)
    private val februar = 1 til 28.februar(2026)
    private val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(fom = januar.fraOgMed, tom = februar.tilOgMed)

    // Ulikt antall dager deler innvilgelsesperiodene i to tilstøtende perioder.
    private val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
        ObjectMother.innvilgelsesperiode(januar, tiltaksdeltakelse, AntallDagerForMeldeperiode(10)),
        ObjectMother.innvilgelsesperiode(februar, tiltaksdeltakelse, AntallDagerForMeldeperiode(8)),
    )

    private fun innvilgelse(barnetillegg: Barnetillegg) = Søknadsbehandlingsresultat.Innvilgelse(
        innvilgelsesperioder = innvilgelsesperioder,
        barnetillegg = barnetillegg,
        omgjørRammevedtak = OmgjørRammevedtak.empty,
    )

    @Test
    fun `barnetillegg V2 må gjelde de samme dagene som innvilgelsesperiodene`() {
        innvilgelsesperioder.perioder shouldBe nonEmptyListOf(januar, februar)

        val barnetillegg = BarnetilleggV2.utenBarnetillegg(innvilgelsesperioder.perioder)
        barnetillegg.innvilgelsesperioder shouldBe nonEmptyListOf(januar.fraOgMed til februar.tilOgMed)
        innvilgelse(barnetillegg)

        shouldThrow<IllegalArgumentException> { innvilgelse(BarnetilleggV2.utenBarnetillegg(nonEmptyListOf(januar))) }
        shouldThrow<IllegalArgumentException> {
            innvilgelse(BarnetilleggV2.utenBarnetillegg(nonEmptyListOf(januar.fraOgMed til 31.mars(2026))))
        }
    }

    @Test
    fun `barnetillegg V1 sjekkes ikke, siden det ikke har egne innvilgelsesperioder`() {
        innvilgelse(BarnetilleggV1(SammenhengendePeriodisering(AntallBarn(1), januar), begrunnelse = null))
    }
}
