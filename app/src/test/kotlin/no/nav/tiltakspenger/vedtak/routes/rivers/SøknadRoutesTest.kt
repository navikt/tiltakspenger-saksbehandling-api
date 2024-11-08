package no.nav.tiltakspenger.vedtak.routes.rivers

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.april
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknadstiltak
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.søknad.SØKNAD_PATH
import no.nav.tiltakspenger.vedtak.routes.søknad.søknadRoutes
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SøknadRoutesTest {
    private companion object {
        val IDENT = Fnr.random()
        const val JOURNALPOSTID = "foobar2"
    }

    @Test
    fun `søknad route + service`() {
        with(TestApplicationContext()) {
            val tac = this
            val søknadId = SøknadId.random()
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        søknadRoutes(
                            søknadService = tac.søknadContext.søknadService,
                            tokenService = tac.tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path(SØKNAD_PATH)
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(),
                ) {
                    setBody(søknadBodyV3(søknadId))
                }.apply {
                    status shouldBe HttpStatusCode.OK
                }
            }

            val actualSøknad = tac.søknadContext.søknadRepo.hentForSøknadId(søknadId)
            actualSøknad shouldBe
                Søknad(
                    versjon = "3",
                    id = actualSøknad.id,
                    journalpostId = JOURNALPOSTID,
                    personopplysninger =
                    Søknad.Personopplysninger(
                        fnr = IDENT,
                        fornavn = "NØDVENDIG",
                        etternavn = "HOFTE",
                    ),
                    tiltak =
                    Søknadstiltak(
                        id = "123",
                        deltakelseFom = 1.april(2025),
                        deltakelseTom = 10.april(2025),
                        typeKode = "Annen utdanning",
                        typeNavn = "Annen utdanning",
                    ),
                    barnetillegg =
                    listOf(
                        Barnetillegg.FraPdl(
                            oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                            fornavn = "INKLUDERENDE",
                            mellomnavn = null,
                            etternavn = "DIVA",
                            fødselsdato = LocalDate.parse("2010-02-13"),
                        ),
                    ),
                    opprettet = actualSøknad.opprettet,
                    tidsstempelHosOss = LocalDateTime.parse("2023-06-14T21:12:08.447993177"),
                    vedlegg = 0,
                    kvp = Søknad.PeriodeSpm.Nei,
                    intro = Søknad.PeriodeSpm.Nei,
                    institusjon = Søknad.PeriodeSpm.Nei,
                    etterlønn = Søknad.JaNeiSpm.Nei,
                    gjenlevendepensjon = Søknad.PeriodeSpm.Nei,
                    alderspensjon = Søknad.FraOgMedDatoSpm.Nei,
                    sykepenger = Søknad.PeriodeSpm.Nei,
                    supplerendeStønadAlder = Søknad.PeriodeSpm.Nei,
                    supplerendeStønadFlyktning = Søknad.PeriodeSpm.Nei,
                    jobbsjansen = Søknad.PeriodeSpm.Nei,
                    trygdOgPensjon = Søknad.PeriodeSpm.Nei,
                )
        }
    }

    private fun søknadBodyV3(søknadId: SøknadId) =
        """
        {
            "versjon": "3",
            "søknadId": "$søknadId",
            "journalpostId": "$JOURNALPOSTID",
            "personopplysninger": {
              "ident": "${IDENT.verdi}",
              "fornavn": "NØDVENDIG",
              "etternavn": "HOFTE"
            },
            "tiltak": {
              "id": "123",
              "arrangør": "Testarrangør",
              "typeKode": "Annen utdanning",
              "typeNavn": "Annen utdanning",
              "deltakelseFom": "2025-04-01",
              "deltakelseTom": "2025-04-10"
            },
            "barnetilleggPdl": [
              {
                "fødselsdato": "2010-02-13",
                "fornavn": "INKLUDERENDE",
                "mellomnavn": null,
                "etternavn": "DIVA",
                "oppholderSegIEØS": {
                  "svar": "Ja"
                }
              }
            ],
            "barnetilleggManuelle": [],
            "vedlegg": 0,
            "kvp": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "intro": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "institusjon": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "etterlønn": {
              "svar": "Nei"
            },
            "gjenlevendepensjon": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "alderspensjon": {
              "svar": "Nei",
              "fom": null
            },
            "sykepenger": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "supplerendeStønadAlder": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "supplerendeStønadFlyktning": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "jobbsjansen": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "trygdOgPensjon": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "opprettet": "2023-06-14T21:12:08.447993177"
        }
        """.trimIndent()
}
