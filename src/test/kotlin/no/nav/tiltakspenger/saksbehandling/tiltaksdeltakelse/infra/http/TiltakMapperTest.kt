package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class TiltakMapperTest {
    @Nested
    inner class MapTiltakMedArrangørnavn {
        val arrangornavn = "Arrangør i Gjøvik AS"
        val tiltakstypenavn = "Arbeidsforberedende trening"
        val tiltakshistorikkDTO = TiltakshistorikkDTO(
            id = UUID.randomUUID().toString(),
            gjennomforing = TiltakshistorikkDTO.GjennomforingDTO(
                id = UUID.randomUUID().toString(),
                visningsnavn = "$tiltakstypenavn hos $arrangornavn",
                arenaKode = TiltakResponsDTO.TiltakType.ARBFORB,
                arrangornavn = arrangornavn,
                typeNavn = tiltakstypenavn,
                deltidsprosent = 100.0,
            ),
            deltakelseFom = 1.januar(2023),
            deltakelseTom = 31.mars(2023),
            deltakelseStatus = TiltakResponsDTO.DeltakerStatusDTO.HAR_SLUTTET,
            antallDagerPerUke = 5.0f,
            deltakelseProsent = 100.0f,
            kilde = TiltakshistorikkDTO.Kilde.KOMET,
        )

        @Test
        fun `viser arrangørens navn`() {
            val tiltaksdeltakelse = mapTiltakMedArrangørnavn(
                harAdressebeskyttelse = false,
                tiltakDTOListe = listOf(tiltakshistorikkDTO),
            )

            tiltaksdeltakelse.first().visningsnavn shouldBe tiltakshistorikkDTO.gjennomforing.visningsnavn
        }

        @Test
        fun `fjerner arrangørens navn om personen har adressebeskyttelse`() {
            val tiltaksdeltakelse = mapTiltakMedArrangørnavn(
                harAdressebeskyttelse = true,
                tiltakDTOListe = listOf(tiltakshistorikkDTO),
            )

            tiltaksdeltakelse.first().visningsnavn shouldBe tiltakshistorikkDTO.gjennomforing.typeNavn
        }
    }
}
