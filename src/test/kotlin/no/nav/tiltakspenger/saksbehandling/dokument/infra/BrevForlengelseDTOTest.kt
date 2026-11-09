package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.json.JSONObject
import org.junit.jupiter.api.Test

class BrevForlengelseDTOTest {

    @Test
    fun `perioder - deles ved årsskiftet og når antall barn endres`() {
        runTest {
            val actual = genererForlengelseBrev(
                hentBrukersNavn = { Navn("Ola", null, "Nordmann") },
                hentSaksbehandlersNavn = { "Saksbehandler Navn" },
                fnr = Fnr.random(),
                saksbehandlerNavIdent = "Z123456",
                beslutterNavIdent = "Z654321",
                saksnummer = Saksnummer("202501301001"),
                vedtaksdato = 31.januar(2025),
                forhåndsvisning = true,
                innvilgelsesperioder = innvilgelsesperioder(Periode(1.desember(2024), 31.januar(2025))),
                barnetillegg = SammenhengendePeriodisering(
                    PeriodeMedVerdi(AntallBarn(2), Periode(1.desember(2024), 31.desember(2024))),
                    PeriodeMedVerdi(AntallBarn(3), Periode(1.januar(2025), 31.januar(2025))),
                ),
                tilleggstekst = null,
                barnSomFyller16 = null,
            )

            JSONObject(actual).getInt("sisteKjenteSatsår") shouldBe 2026
            JSONObject(actual).getBoolean("harBarnetillegg") shouldBe true

            //language=json
            JSONObject(actual).getJSONArray("perioder").toString() shouldEqualJson """
                [
                  {
                    "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2025-01-31"},
                    "beløp": [
                      {
                        "periode": {"fraOgMed": "2024-12-01", "tilOgMed": "2024-12-31"},
                        "antallBarn": 2,
                        "tiltakspenger": 285,
                        "barnetilleggBeløp": 106,
                        "total": 391
                      },
                      {
                        "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-01-31"},
                        "antallBarn": 3,
                        "tiltakspenger": 298,
                        "barnetilleggBeløp": 165,
                        "total": 463
                      }
                    ]
                  }
                ]
            """.trimIndent()
        }
    }
}
