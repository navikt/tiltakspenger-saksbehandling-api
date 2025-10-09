package no.nav.tiltakspenger.saksbehandling.person.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import org.junit.jupiter.api.Test

internal class EnkelPersonMapperTest {

    @Test
    fun test() {
        val fnr = Fnr.random()
        //language=JSON
        """
{
  "hentPerson": {
    "adressebeskyttelse": [],
    "navn": [
      {
        "fornavn": "UFØLSOM",
        "mellomnavn": null,
        "etternavn": "FAKKEL",
        "folkeregistermetadata": {
          "aarsak": null,
          "ajourholdstidspunkt": "2024-09-18T12:43:02",
          "gyldighetstidspunkt": "1971-11-15T00:00",
          "kilde": "Dolly",
          "opphoerstidspunkt": null,
          "sekvens": null
        },
        "metadata": {
          "endringer": [
            {
              "kilde": "Dolly",
              "registrert": "2024-09-18T12:43:02",
              "registrertAv": "Folkeregisteret",
              "systemkilde": "FREG",
              "type": "OPPRETT"
            }
          ],
          "master": "FREG"
        }
      }
    ],
    "foedselsdato": [
        {
          "foedselsdato": "1995-10-16",
          "folkeregistermetadata": {
            "aarsak": null,
            "ajourholdstidspunkt": "2022-06-17T09:32:15",
            "gyldighetstidspunkt": "2022-06-17T09:32:15",
            "kilde": "Dolly",
            "opphoerstidspunkt": null,
            "sekvens": null
          },
          "metadata": {
            "endringer": [
              {
                "kilde": "Dolly",
                "registrert": "2022-06-17T09:32:15",
                "registrertAv": "Folkeregisteret",
                "systemkilde": "FREG",
                "type": "OPPRETT"
              }
            ],
            "master": "FREG"
          }
        }
      ]
  }
}
        """.trimIndent().toEnkelPerson(fnr) shouldBe EnkelPerson(
            fnr = fnr,
            fødselsdato = 16.oktober(1995),
            fornavn = "UFØLSOM",
            mellomnavn = null,
            etternavn = "FAKKEL",
            fortrolig = false,
            strengtFortrolig = false,
            strengtFortroligUtland = false,
            dødsdato = null,
        )
    }
}
