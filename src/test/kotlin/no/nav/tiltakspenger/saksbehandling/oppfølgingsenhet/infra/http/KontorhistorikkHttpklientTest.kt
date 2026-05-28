package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.withWireMockServer
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class KontorhistorikkHttpklientTest {

    /**
     * Vi sammenligner mot hele lista bevisst, slik at testen brekker dersom vi legger til (eller mister) felter på
     * [Kontorhistorikkinnslag] uten å tenke gjennom personvernkonsekvenser.
     */
    @Test
    fun `parser kontorHistorikk-respons og mapper alle innslag uten filtrering`() {
        val fnr = Fnr.random()
        val body = lagKontorhistorikkResponseBody(
            ident = fnr.verdi,
            innslag = listOf(
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    kontorId = "0123",
                    kontorNavn = "NAV Oslo",
                    kontorType = "ARBEIDSOPPFOLGING",
                    endretTidspunkt = "2024-05-01T10:15:30+02:00[Europe/Oslo]",
                ),
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    kontorId = "0456",
                    kontorNavn = null,
                    kontorType = "ARENA",
                    endretTidspunkt = "2024-03-01T08:00:00+01:00[Europe/Oslo]",
                ),
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    kontorId = "9999",
                    kontorNavn = "Skal IKKE filtreres ut",
                    kontorType = "GEOGRAFISK_TILKNYTNING",
                    endretTidspunkt = "2024-04-01T09:00:00+02:00[Europe/Oslo]",
                ),
            ),
        )

        medWiremock(body) { klient ->
            runTest {
                // Vi sender med litt metadata for å verifisere at signaturen tar dem (de logges, men det
                // tester vi ikke direkte her).
                klient.hentKontorhistorikk(
                    fnr = fnr,
                    sakId = "sak-1",
                    saksnummer = "2024-1",
                    rammebehandlingId = "ram-1",
                    meldekortbehandlingId = "mel-1",
                ).map { it.kontorhistorikk } shouldBe Kontorhistorikk(
                    listOf(
                        Kontorhistorikkinnslag(
                            kontorId = "0123",
                            kontorNavn = "NAV Oslo",
                            kontorType = KontorType.ARBEIDSOPPFOLGING,
                            endretTidspunkt = LocalDateTime.parse("2024-05-01T10:15:30"),
                        ),
                        Kontorhistorikkinnslag(
                            kontorId = "0456",
                            kontorNavn = null,
                            kontorType = KontorType.ARENA,
                            endretTidspunkt = LocalDateTime.parse("2024-03-01T08:00:00"),
                        ),
                        Kontorhistorikkinnslag(
                            kontorId = "9999",
                            kontorNavn = "Skal IKKE filtreres ut",
                            kontorType = KontorType.GEOGRAFISK_TILKNYTNING,
                            endretTidspunkt = LocalDateTime.parse("2024-04-01T09:00:00"),
                        ),
                    ),
                ).right()
            }
        }
    }

    @Test
    fun `tom historikk gir tom liste`() {
        val fnr = Fnr.random()
        val body = lagKontorhistorikkResponseBody(ident = fnr.verdi, innslag = emptyList())

        medWiremock(body) { klient ->
            runTest {
                klient.hentKontorhistorikk(fnr).map { it.kontorhistorikk } shouldBe Kontorhistorikk(emptyList()).right()
            }
        }
    }

    @Test
    fun `ident-mismatch i respons gir Left IdentMismatch`() {
        val forespurt = Fnr.random()
        val annenIdent = Fnr.random().verdi
        val body = lagKontorhistorikkResponseBody(
            ident = annenIdent,
            innslag = listOf(kontorhistorikkInnslagJson(ident = annenIdent)),
        )

        medWiremock(body) { klient ->
            runTest {
                klient.hentKontorhistorikk(forespurt).leftOrNull()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.IdentMismatch>()
            }
        }
    }

    @Test
    fun `non-200 fra tjenesten gir Left UventetHttpStatus`() {
        val fnr = Fnr.random()
        medWiremock(body = """{"message": "noe gikk galt"}""", statusCode = 503) { klient ->
            runTest {
                klient.hentKontorhistorikk(fnr).leftOrNull()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.UventetHttpStatus>()
                    .status shouldBe 503
            }
        }
    }

    @Test
    fun `GraphQL errors i respons gir Left GraphQlFeil`() {
        val fnr = Fnr.random()
        val body = """
            {
              "errors": [{ "message": "noe gikk galt" }],
              "data": null
            }
        """.trimIndent()

        medWiremock(body) { klient ->
            runTest {
                klient.hentKontorhistorikk(fnr).leftOrNull()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.GraphQlFeil>()
            }
        }
    }

    @Test
    fun `ukjent kontorType i respons gir Left KallFeilet`() {
        val fnr = Fnr.random()
        val body = lagKontorhistorikkResponseBody(
            ident = fnr.verdi,
            innslag = listOf(
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    kontorType = "EN_HELT_NY_TYPE",
                ),
            ),
        )

        medWiremock(body) { klient ->
            runTest {
                klient.hentKontorhistorikk(fnr).leftOrNull()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
            }
        }
    }

    @Test
    fun `mapper alle kjente kontorType-verdier til domene-enum`() {
        val fnr = Fnr.random()
        val body = lagKontorhistorikkResponseBody(
            ident = fnr.verdi,
            innslag = listOf(
                kontorhistorikkInnslagJson(ident = fnr.verdi, kontorType = "ARBEIDSOPPFOLGING"),
                kontorhistorikkInnslagJson(ident = fnr.verdi, kontorType = "ARENA"),
                kontorhistorikkInnslagJson(ident = fnr.verdi, kontorType = "GEOGRAFISK_TILKNYTNING"),
            ),
        )

        medWiremock(body) { klient ->
            runTest {
                val resultat = klient.hentKontorhistorikk(fnr)
                resultat.isRight() shouldBe true
                resultat.getOrNull()!!.kontorhistorikk.innslag.map { it.kontorType } shouldBe listOf(
                    KontorType.ARBEIDSOPPFOLGING,
                    KontorType.ARENA,
                    KontorType.GEOGRAFISK_TILKNYTNING,
                )
            }
        }
    }

    @Test
    fun `konverterer endretTidspunkt fra UTC til Europe-Oslo wall-clock`() {
        val fnr = Fnr.random()
        // Sommertid: 08-00 UTC = 10-00 Oslo. Vintertid: 08-00 UTC = 09-00 Oslo.
        val body = lagKontorhistorikkResponseBody(
            ident = fnr.verdi,
            innslag = listOf(
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    kontorId = "sommer",
                    kontorType = "ARBEIDSOPPFOLGING",
                    endretTidspunkt = "2024-07-01T08:00:00Z[UTC]",
                ),
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    kontorId = "vinter",
                    kontorType = "ARBEIDSOPPFOLGING",
                    endretTidspunkt = "2024-01-15T08:00:00Z[UTC]",
                ),
            ),
        )

        medWiremock(body) { klient ->
            runTest {
                val innslag = klient.hentKontorhistorikk(fnr).getOrNull()!!.kontorhistorikk.innslag
                innslag.single { it.kontorId == "sommer" }.endretTidspunkt shouldBe
                    LocalDateTime.parse("2024-07-01T10:00:00")
                innslag.single { it.kontorId == "vinter" }.endretTidspunkt shouldBe
                    LocalDateTime.parse("2024-01-15T09:00:00")
            }
        }
    }

    @Test
    fun `ugyldig endretTidspunkt-format gir Left KallFeilet`() {
        val fnr = Fnr.random()
        val body = lagKontorhistorikkResponseBody(
            ident = fnr.verdi,
            innslag = listOf(
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    endretTidspunkt = "ikke-en-dato",
                ),
            ),
        )

        medWiremock(body) { klient ->
            runTest {
                klient.hentKontorhistorikk(fnr).leftOrNull()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
            }
        }
    }

    /**
     * Verifiserer at vi takler ISO-8601-varianter et "vanlig" API kan finne på å returnere. Alle inputene
     * her representerer det samme øyeblikket (08:00 UTC = 10:00 Oslo i sommertid), så vi forventer samme
     * vegg-klokke-tidspunkt i Oslo uansett serialiseringsstil.
     */
    @Test
    fun `parser ulike ISO-8601 sone-varianter til samme Oslo-wall-clock`() {
        val forventet = LocalDateTime.parse("2024-07-01T10:00:00")
        val varianter = listOf(
            // Java sin egen ZonedDateTime.toString() med Europe/Oslo
            "2024-07-01T10:00:00+02:00[Europe/Oslo]",
            // ZonedDateTime.toString() med UTC
            "2024-07-01T08:00:00Z[UTC]",
            // OffsetDateTime / ISO-instant med Z, uten zone-id
            "2024-07-01T08:00:00Z",
            // OffsetDateTime med eksplisitt +00:00 i stedet for Z
            "2024-07-01T08:00:00+00:00",
            // Offset uten zone-id (typisk fra mange JSON-APIer)
            "2024-07-01T10:00:00+02:00",
            // Med fraksjonelle sekunder (vi kapper dem siden domenet vårt er sekund-presisjon)
            "2024-07-01T08:00:00.000Z",
            // Et annet offset (New York sommertid) som peker på samme instant
            "2024-07-01T04:00:00-04:00",
        )

        varianter.forEach { input ->
            val fnr = Fnr.random()
            val body = lagKontorhistorikkResponseBody(
                ident = fnr.verdi,
                innslag = listOf(kontorhistorikkInnslagJson(ident = fnr.verdi, endretTidspunkt = input)),
            )

            medWiremock(body) { klient ->
                runTest {
                    val faktisk = klient.hentKontorhistorikk(fnr).getOrNull()!!
                        .kontorhistorikk
                        .innslag
                        .single()
                        .endretTidspunkt
                    withClue("input=$input") {
                        // Fraksjonelle sekunder blir bevart i LocalDateTime, men er 0 i alle våre eksempler.
                        faktisk shouldBe forventet
                    }
                }
            }
        }
    }

    /**
     * Et tidsstempel uten sone/offset (ren `LocalDateTime`) er tvetydig – vi vet ikke hva avsender
     * mente. Vi velger eksplisitt å feile heller enn å gjette på Europe/Oslo, slik at vi ikke ender
     * opp med stille feil dersom APIet endrer kontrakt.
     */
    @Test
    fun `tidsstempel uten sone gir Left KallFeilet`() {
        val fnr = Fnr.random()
        val body = lagKontorhistorikkResponseBody(
            ident = fnr.verdi,
            innslag = listOf(
                kontorhistorikkInnslagJson(
                    ident = fnr.verdi,
                    endretTidspunkt = "2024-07-01T10:00:00",
                ),
            ),
        )

        medWiremock(body) { klient ->
            runTest {
                klient.hentKontorhistorikk(fnr).leftOrNull()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
            }
        }
    }

    private fun medWiremock(
        body: String,
        statusCode: Int = 200,
        block: (KontorhistorikkHttpklient) -> Unit,
    ) {
        withWireMockServer { wiremock ->
            wiremock.post {
                url contains "/graphql"
            } returns {
                this.statusCode = statusCode
                header = "Content-Type" to "application/json"
                this.body = body
            }
            block(
                KontorhistorikkHttpklient(
                    baseUrl = wiremock.baseUrl(),
                    getToken = { ObjectMother.accessToken() },
                ),
            )
        }
    }

    private fun lagKontorhistorikkResponseBody(
        ident: String,
        innslag: List<String> = listOf(kontorhistorikkInnslagJson(ident = ident)),
    ): String =
        """
        {
          "data": {
            "kontorHistorikk": [${innslag.joinToString(",")}]
          }
        }
        """.trimIndent()

    /**
     * Bygger ett JSON-innslag. [kontorNavn] er nullable: `null` blir til JSON `null`, en streng blir til
     * en JSON-streng.
     */
    private fun kontorhistorikkInnslagJson(
        ident: String,
        kontorId: String = "0123",
        kontorNavn: String? = "NAV Oslo",
        kontorType: String = "ARBEIDSOPPFOLGING",
        endretTidspunkt: String = "2024-05-01T10:15:30+02:00[Europe/Oslo]",
    ): String {
        val kontorNavnJson = kontorNavn?.let { "\"$it\"" } ?: "null"
        return """
        {
          "ident": "$ident",
          "kontorId": "$kontorId",
          "kontorNavn": $kontorNavnJson,
          "kontorType": "$kontorType",
          "endretTidspunkt": "$endretTidspunkt"
        }
        """.trimIndent()
    }
}
