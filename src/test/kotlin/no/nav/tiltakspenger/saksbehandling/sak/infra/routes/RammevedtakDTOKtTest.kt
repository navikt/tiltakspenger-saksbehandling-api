package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.TidslinjeResultat
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.toTidslinjeElementDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RammevedtakDTOKtTest {

    @Test
    fun `mapper et rammevedtak  til et TidslinjeElementDTO`() {
        val innvilgelsesperiode = ObjectMother.vedtaksperiode()
        val rammevedtak = ObjectMother.nyRammevedtakInnvilgelse(
            innvilgelsesperioder = listOf(
                innvilgelsesperiodeKommando(innvilgelsesperiode = innvilgelsesperiode),
            ),
        )
        val tidslinjeElementDTO = rammevedtak.toTidslinjeElementDto(innvilgelsesperiode)

        tidslinjeElementDTO.size shouldBe 1
        tidslinjeElementDTO.first().periode shouldBe innvilgelsesperiode.toDTO()
        tidslinjeElementDTO.first().tidslinjeResultat shouldBe TidslinjeResultat.SØKNADSBEHANDLING_INNVILGELSE
        tidslinjeElementDTO.first().rammevedtak.barnetillegg shouldBe BarnetilleggDTO(
            perioder = listOf(BarnetilleggPeriodeDTO(antallBarn = 0, periode = innvilgelsesperiode.toDTO())),
            begrunnelse = null,
        )
    }

    @Nested
    inner class Omgjøringsvedtak {
        @Test
        fun `mapper et omgjøringsvedtak til et TidslinjeElementDTO som går over hele den opprinnelige perioden`() {
            val innvilgelsesperiode = ObjectMother.vedtaksperiode()
            val rammevedtak = ObjectMother.nyRammevedtakOmgjøring(
                søknadsbehandlingInnvilgelsesperiode = innvilgelsesperiode,
                omgjøringInnvilgelsesperiode = innvilgelsesperiode,
            )

            val tidslinjeElementDTO = rammevedtak.toTidslinjeElementDto(innvilgelsesperiode)
            tidslinjeElementDTO.size shouldBe 1
            tidslinjeElementDTO.first().periode shouldBe innvilgelsesperiode.toDTO()
            tidslinjeElementDTO.first().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_INNVILGELSE
            tidslinjeElementDTO.first().rammevedtak.barnetillegg shouldBe BarnetilleggDTO(
                perioder = listOf(BarnetilleggPeriodeDTO(antallBarn = 0, periode = innvilgelsesperiode.toDTO())),
                begrunnelse = null,
            )
        }

        @Test
        fun `omgjøring som fører til 1 opphørsperiode`() {
            val innvilgelsesperiode = ObjectMother.vedtaksperiode()
            val omgjøringInnvilgelsesperiode = innvilgelsesperiode.plusFraOgMed(1)
            val rammevedtak = ObjectMother.nyRammevedtakOmgjøring(
                søknadsbehandlingInnvilgelsesperiode = innvilgelsesperiode,
                omgjøringInnvilgelsesperiode = omgjøringInnvilgelsesperiode,
            )

            val tidslinjeElementDTO = rammevedtak.toTidslinjeElementDto(innvilgelsesperiode)
            tidslinjeElementDTO.size shouldBe 2
            val forventedeOpphørsperiode =
                Periode(innvilgelsesperiode.fraOgMed, tilOgMed = innvilgelsesperiode.fraOgMed)
            tidslinjeElementDTO.first().periode shouldBe forventedeOpphørsperiode.toDTO()
            tidslinjeElementDTO.first().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_OPPHØR
            tidslinjeElementDTO.first().rammevedtak.barnetillegg shouldBe null

            tidslinjeElementDTO.last().periode shouldBe omgjøringInnvilgelsesperiode.toDTO()
            tidslinjeElementDTO.last().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_INNVILGELSE
            tidslinjeElementDTO.last().rammevedtak.barnetillegg shouldBe BarnetilleggDTO(
                perioder = listOf(
                    BarnetilleggPeriodeDTO(
                        antallBarn = 0,
                        periode = omgjøringInnvilgelsesperiode.toDTO(),
                    ),
                ),
                begrunnelse = null,
            )
        }

        @Test
        fun `omgjøring som fører til 2 opphørsperioder`() {
            val innvilgelsesperiode = ObjectMother.vedtaksperiode()
            val omgjøringInnvilgelsesperiode = innvilgelsesperiode.plusFraOgMed(1).minusTilOgMed(1)
            val rammevedtak = ObjectMother.nyRammevedtakOmgjøring(
                søknadsbehandlingInnvilgelsesperiode = innvilgelsesperiode,
                omgjøringInnvilgelsesperiode = omgjøringInnvilgelsesperiode,
            )

            val tidslinjeElementDTO = rammevedtak.toTidslinjeElementDto(innvilgelsesperiode)
            tidslinjeElementDTO.size shouldBe 3
            val forventedeOpphørsperiodeFørsteDto =
                Periode(innvilgelsesperiode.fraOgMed, tilOgMed = innvilgelsesperiode.fraOgMed)
            tidslinjeElementDTO.first().periode shouldBe forventedeOpphørsperiodeFørsteDto.toDTO()
            tidslinjeElementDTO.first().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_OPPHØR
            tidslinjeElementDTO.first().rammevedtak.barnetillegg shouldBe null

            tidslinjeElementDTO[1].periode shouldBe omgjøringInnvilgelsesperiode.toDTO()
            tidslinjeElementDTO[1].tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_INNVILGELSE
            tidslinjeElementDTO[1].rammevedtak.barnetillegg shouldBe BarnetilleggDTO(
                perioder = listOf(
                    BarnetilleggPeriodeDTO(
                        antallBarn = 0,
                        periode = omgjøringInnvilgelsesperiode.toDTO(),
                    ),
                ),
                begrunnelse = null,
            )

            val forventedeOpphørsperiodeSisteDto =
                Periode(innvilgelsesperiode.tilOgMed, tilOgMed = innvilgelsesperiode.tilOgMed)
            tidslinjeElementDTO.last().periode shouldBe forventedeOpphørsperiodeSisteDto.toDTO()
            tidslinjeElementDTO.last().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_OPPHØR
            tidslinjeElementDTO.last().rammevedtak.barnetillegg shouldBe null
        }
    }

    @Test
    fun `avslag kaster exception`() {
        assertThrows<IllegalStateException> {
            val avslagsperiode = ObjectMother.vedtaksperiode()
            val rammevedtak = ObjectMother.nyRammevedtakAvslag(avslagsperiode = avslagsperiode)
            rammevedtak.toTidslinjeElementDto(avslagsperiode)
        }
    }
}
