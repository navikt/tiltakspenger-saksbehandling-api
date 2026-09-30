package no.nav.tiltakspenger.saksbehandling.person.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import org.junit.jupiter.api.Test

class AdressebeskyttelseMapperTest {

    private val utland = Fnr.random()
    private val ugradert = Fnr.random()
    private val ikkeFunnet = Fnr.random()
    private val meldtAvBrukerSelv = Fnr.random()

    @Test
    fun `mapper graderingen, og utelater personer som ikke finnes eller ikke kan avklares`() {
        //language=JSON
        """
        {
          "hentPersonBolk": [
            {
              "ident": "${utland.verdi}",
              "person": {
                "adressebeskyttelse": [
                  {
                    "gradering": "STRENGT_FORTROLIG_UTLAND",
                    "folkeregistermetadata": null,
                    "metadata": {
                      "endringer": [
                        { "kilde": "Dolly", "registrert": "2024-09-18T12:43:02", "registrertAv": "Folkeregisteret", "systemkilde": "PDL", "type": "OPPRETT" }
                      ],
                      "master": "PDL"
                    }
                  }
                ]
              },
              "code": "ok"
            },
            {
              "ident": "${ugradert.verdi}",
              "person": { "adressebeskyttelse": [] },
              "code": "ok"
            },
            {
              "ident": "${ikkeFunnet.verdi}",
              "person": null,
              "code": "not_found"
            },
            {
              "ident": "${meldtAvBrukerSelv.verdi}",
              "person": {
                "adressebeskyttelse": [
                  {
                    "gradering": "FORTROLIG",
                    "folkeregistermetadata": null,
                    "metadata": {
                      "endringer": [
                        { "kilde": "Bruker selv", "registrert": "2024-09-18T12:43:02", "registrertAv": "Bruker", "systemkilde": "PDL", "type": "OPPRETT" }
                      ],
                      "master": "PDL"
                    }
                  }
                ]
              },
              "code": "ok"
            }
          ]
        }
        """.trimIndent().toAdressebeskyttelseBolk() shouldBe mapOf(
            utland to Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND,
            ugradert to Adressebeskyttelse.UGRADERT,
        )
    }
}
