package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.person.Navn
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevKlageAvvisningDTOTest {

    @Test
    fun `uten tildelt saksbehandler serialiseres navnet som null og navneoppslaget kalles ikke`() {
        runTest {
            val fnr = Fnr.random()
            val actual = BrevKlageAvvisningDTO.create(
                tilleggstekst = Brevtekster(listOf(TittelOgTekst("Hva klagesaken gjelder", "Vi viser til klagen din."))),
                hentBrukersNavn = { _ -> Navn("Fornavn", null, "Etternavn") },
                hentSaksbehandlersNavn = { error("Navneoppslaget skal ikke kalles når ingen saksbehandler er tildelt") },
                saksbehandlerNavIdent = null,
                saksnummer = Saksnummer.genererSaknummer(LocalDate.now(fixedClock), "2000"),
                forhåndsvisning = true,
                datoForUtsending = LocalDate.now(fixedClock),
                fnr = fnr,
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
                  "saksbehandlerNavn":null,
                  "datoForUtsending":"1. januar 2025",
                  "tilleggstekst":[
                    {
                      "tittel":"Hva klagesaken gjelder",
                      "tekst":"Vi viser til klagen din."
                    }
                  ],
                  "forhandsvisning":true
                }
            """.trimIndent()

            actual.shouldEqualJson(expected)
        }
    }
}
