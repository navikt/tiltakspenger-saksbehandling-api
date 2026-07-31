package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.TidslinjeResultat
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.toTidslinjeElementDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
        tidslinjeElementDTO.first().rammevedtakId shouldBe rammevedtak.id.toString()
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
            tidslinjeElementDTO.first().rammevedtakId shouldBe rammevedtak.id.toString()
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

            tidslinjeElementDTO.last().periode shouldBe omgjøringInnvilgelsesperiode.toDTO()
            tidslinjeElementDTO.last().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_INNVILGELSE

            tidslinjeElementDTO.map { it.rammevedtakId }.distinct() shouldBe listOf(rammevedtak.id.toString())
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

            tidslinjeElementDTO[1].periode shouldBe omgjøringInnvilgelsesperiode.toDTO()
            tidslinjeElementDTO[1].tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_INNVILGELSE

            val forventedeOpphørsperiodeSisteDto =
                Periode(innvilgelsesperiode.tilOgMed, tilOgMed = innvilgelsesperiode.tilOgMed)
            tidslinjeElementDTO.last().periode shouldBe forventedeOpphørsperiodeSisteDto.toDTO()
            tidslinjeElementDTO.last().tidslinjeResultat shouldBe TidslinjeResultat.OMGJØRING_OPPHØR

            tidslinjeElementDTO.map { it.rammevedtakId }.distinct() shouldBe listOf(rammevedtak.id.toString())
        }

        @Test
        fun `omgjøring med flere innvilgelsesperioder gir et element per innvilgelse og per hull`() {
            val innvilgelsesperiode = Periode(1.januar(2023), 31.januar(2023))
            val førsteOmgjøringsperiode = Periode(5.januar(2023), 10.januar(2023))
            val andreOmgjøringsperiode = Periode(20.januar(2023), 25.januar(2023))

            val rammevedtak = ObjectMother.nyRammevedtakOmgjøring(
                søknadsbehandlingInnvilgelsesperiode = innvilgelsesperiode,
                omgjøringInnvilgelsesperioder = listOf(førsteOmgjøringsperiode, andreOmgjøringsperiode),
            )

            val tidslinjeElementDTO = rammevedtak.toTidslinjeElementDto(innvilgelsesperiode)

            tidslinjeElementDTO.map { it.periode to it.tidslinjeResultat } shouldBe listOf(
                Periode(1.januar(2023), 4.januar(2023)).toDTO() to TidslinjeResultat.OMGJØRING_OPPHØR,
                førsteOmgjøringsperiode.toDTO() to TidslinjeResultat.OMGJØRING_INNVILGELSE,
                Periode(11.januar(2023), 19.januar(2023)).toDTO() to TidslinjeResultat.OMGJØRING_OPPHØR,
                andreOmgjøringsperiode.toDTO() to TidslinjeResultat.OMGJØRING_INNVILGELSE,
                Periode(26.januar(2023), 31.januar(2023)).toDTO() to TidslinjeResultat.OMGJØRING_OPPHØR,
            )

            tidslinjeElementDTO.map { it.rammevedtakId }.distinct() shouldBe listOf(rammevedtak.id.toString())
        }
    }

    @Test
    fun `avslag kaster exception`() {
        shouldThrow<IllegalStateException> {
            val avslagsperiode = ObjectMother.vedtaksperiode()
            val rammevedtak = ObjectMother.nyRammevedtakAvslag(avslagsperiode = avslagsperiode)
            rammevedtak.toTidslinjeElementDto(avslagsperiode)
        }
    }
}
