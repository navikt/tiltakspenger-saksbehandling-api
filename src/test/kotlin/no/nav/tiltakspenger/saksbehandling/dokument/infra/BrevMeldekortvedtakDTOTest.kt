package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import org.junit.jupiter.api.Test

class BrevMeldekortvedtakDTOTest {

    @Test
    fun `DagSammenligning DTO mapper`() {
        val dagSammenligning = SammenligningAvBeregninger.DagSammenligning(
            dato = 1.desember(2025),
            status = SammenligningAvBeregninger.ForrigeOgGjeldende(
                forrige = MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket.create(
                    dato = 1.desember(2025),
                    tiltakstype = TiltakstypeSomGirRett.ARBEIDSFORBEREDENDE_TRENING,
                    antallBarn = AntallBarn(5),
                ),
                gjeldende = MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav.create(
                    dato = 1.desember(2025),
                    tiltakstype = TiltakstypeSomGirRett.ARBEIDSTRENING,
                    antallBarn = AntallBarn(3),
                ),
            ),
            beløp = SammenligningAvBeregninger.ForrigeOgGjeldende(
                forrige = 100,
                gjeldende = 1000,
            ),
            barnetillegg = SammenligningAvBeregninger.ForrigeOgGjeldende(
                forrige = 50,
                gjeldende = 500,
            ),
            prosent = SammenligningAvBeregninger.ForrigeOgGjeldende(
                forrige = 10,
                gjeldende = 50,
            ),
        )

        val actual = dagSammenligning.toDto()

        actual shouldBe BrevMeldekortvedtakDTO.DagSammenligningDTO(
            dato = "mandag 1. desember",
            status = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(
                "Deltatt",
                "Fravær godkjent av Nav",
                true,
            ),
            beløp = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(100, 1000, true),
            barnetillegg = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(50, 500, true),
            prosent = BrevMeldekortvedtakDTO.ForrigeOgGjeldendeDTO(10, 50, true),
            harEndretSeg = true,
        )
    }
}
