package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BarnetilleggDTOSladdingTest {

    @Test
    fun `personopplysningene om barna og begrunnelsen sladdes mens øvrige felter forblir uendret`() {
        val barnetilleggDTO = barnetilleggV2DTO()

        barnetilleggDTO.sladdet() shouldBe barnetilleggDTO.copy(
            begrunnelse = SladdetVerdi,
            barn = barnetilleggDTO.barn!!.map {
                it.copy(
                    fnr = SladdetVerdi,
                    fornavn = SladdetVerdi,
                    mellomnavn = SladdetVerdi,
                    etternavn = SladdetVerdi,
                    fødselsdato = SladdetVerdi,
                )
            },
        )
    }

    @Test
    fun `barna sladdes uansett om de er fra søknaden eller lagt til av saksbehandler, og med eller uten fødselsnummer`() {
        val barn = barnetilleggV2DTO().sladdet().barn!!

        barn.map { it.fnr } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barn.map { it.fornavn } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barn.map { it.mellomnavn } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barn.map { it.etternavn } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barn.map { it.fødselsdato } shouldBe listOf(SladdetVerdi, SladdetVerdi)
        barn.map { it.kilde } shouldBe listOf(BarnKildeDTO.SØKNAD, BarnKildeDTO.SAKSBEHANDLER)
    }

    @Test
    fun `V1 har ingen barn å sladde, og får bare begrunnelsen sladdet`() {
        val barnetilleggDTO = ObjectMother.barnetillegg(begrunnelse = Begrunnelse.createOrThrow("Begrunnelse")).toBarnetilleggDTO()

        barnetilleggDTO.sladdet() shouldBe barnetilleggDTO.copy(begrunnelse = SladdetVerdi)
    }

    private fun barnetilleggV2DTO(): BarnetilleggDTO = ObjectMother.barnetilleggV2(
        begrunnelse = Begrunnelse.createOrThrow("Begrunnelse"),
        barn = listOf(
            ObjectMother.barnMedBarnetillegg(
                fnr = Fnr.random(),
                fødselsdato = 1.januar(2015),
                kilde = BarnMedBarnetillegg.Kilde.Søknad(SøknadId.random()),
            ),
            ObjectMother.barnMedBarnetillegg(fnr = null, fornavn = "Manuelt", fødselsdato = 1.januar(2020)),
        ),
    ).toBarnetilleggDTO()
}
