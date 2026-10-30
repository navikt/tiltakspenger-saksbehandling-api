package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevKlageForvaltningsmeldingDTOTest {

    @Test
    fun `datoene serialiseres som ISO`() {
        runTest {
            val fnr = Fnr.random()
            val actual = BrevKlageForvaltningsmeldingDTO.create(
                tilleggstekst = Brevtekster(
                    listOf(
                        TittelOgTekst(
                            "Svartid",
                            "Saken vil bli behandlet innen ca. 3 måneder.",
                        ),
                    ),
                ),
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { _ -> "Saksbehandler Saksbehandleren" },
                saksbehandlerNavIdent = "Z123456",
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = false,
                datoForUtsending = LocalDate.now(fixedClock),
                fnr = fnr,
                innsendingsdato = 18.februar(2024),
            )

            //language=json
            val expected = """
                {
                  "personalia":{
                    "ident":"${fnr.verdi}",
                    "fornavn":"Fornavn",
                    "etternavn":"Etternavn"
                  },
                  "saksnummer":"202501012000",
                  "saksbehandlerNavn":"Saksbehandler Saksbehandleren",
                  "datoForUtsending":"2025-01-01",
                  "tilleggstekst":[
                    {
                      "tittel":"Svartid",
                      "tekst":"Saken vil bli behandlet innen ca. 3 måneder."
                    }
                  ],
                  "forhandsvisning":false,
                  "innsendingsdato":"2024-02-18"
                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }
}
