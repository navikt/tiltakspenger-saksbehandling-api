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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tiltakspenger.felles.april
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SøknadsTiltak
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Vedlegg
import no.nav.tiltakspenger.saksbehandling.service.sak.SakServiceImpl
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import no.nav.tiltakspenger.vedtak.routes.jacksonSerialization
import no.nav.tiltakspenger.vedtak.routes.rivers.søknad.søknadRoutes
import no.nav.tiltakspenger.vedtak.routes.rivers.søknad.søknadpath
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SøknadRoutesTest {
    private companion object {
        const val IDENT = "04927799109"
        const val JOURNALPOSTID = "foobar2"
    }

    private val mockSakService = mockk<SakServiceImpl>(relaxed = true)

    @Test
    fun `sjekk at kall til river søknad route mapper søknad riktig og kaller mottak`() {
        val søknad = slot<Søknad>()
        every { mockSakService.motta(capture(søknad)) } returns ObjectMother.tomSak()

        testApplication {
            application {
                // vedtakTestApi()
                jacksonSerialization()
                routing {
                    søknadRoutes(
                        sakService = mockSakService,
                    )
                }
            }
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path(søknadpath)
                },
            ) {
                setBody(søknadBodyV3)
            }
                .apply {
                    status shouldBe HttpStatusCode.OK
                }
        }

        søknad.captured shouldBe Søknad(
            versjon = "3",
            id = søknad.captured.id,
            søknadId = "735ac33a-bf3b-43c0-a331-e9ac99bdd6f8",
            journalpostId = JOURNALPOSTID,
            dokumentInfoId = "987",
            filnavn = "tiltakspengersoknad.json",
            personopplysninger = Søknad.Personopplysninger(
                ident = IDENT,
                fornavn = "NØDVENDIG",
                etternavn = "HOFTE",
            ),
            tiltak = SøknadsTiltak(
                id = "123",
                deltakelseFom = 1.april(2025),
                deltakelseTom = 10.april(2025),
                arrangør = "Testarrangør",
                typeKode = "Annen utdanning",
                typeNavn = "Annen utdanning",
            ),
            barnetillegg = listOf(
                Barnetillegg.FraPdl(
                    oppholderSegIEØS = Søknad.JaNeiSpm.Ja,
                    fornavn = "INKLUDERENDE",
                    mellomnavn = null,
                    etternavn = "DIVA",
                    fødselsdato = LocalDate.parse("2010-02-13"),
                ),
            ),
            opprettet = søknad.captured.opprettet,
            tidsstempelHosOss = LocalDateTime.parse("2023-06-14T21:12:08.447993177"),
            vedlegg = listOf(
                Vedlegg(
                    journalpostId = "123",
                    dokumentInfoId = "456",
                    filnavn = "tiltakspengersoknad.json",

                ),
            ),
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

    private val søknadBodyV3 = """
        {
            "versjon": "3",
            "søknadId": "735ac33a-bf3b-43c0-a331-e9ac99bdd6f8",
            "dokInfo": {
              "journalpostId": "$JOURNALPOSTID",
              "dokumentInfoId": "987",
              "filnavn": "tiltakspengersoknad.json"
            },
            "personopplysninger": {
              "ident": "$IDENT",
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
            "vedlegg": [
              {
                "journalpostId": "123",
                "dokumentInfoId": "456",
                "filnavn": "tiltakspengersoknad.json"
              }
            ],
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
