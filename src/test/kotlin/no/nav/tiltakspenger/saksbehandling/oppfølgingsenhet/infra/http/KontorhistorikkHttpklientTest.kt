package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.right
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteKontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

internal class KontorhistorikkHttpklientTest {
    private val baseUrl = "http://ao-oppfolgingskontor.test"
    private val fnr = Fnr.random()

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = KontorhistorikkHttpklient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = ObjectMother.clock,
        transport = transport,
    )

    private fun httpKlientMedInnslag(innslag: List<KontorhistorikkDto>?) = FakeHttpTransport().apply {
        leggIKøJson(GraphQlResponse(data = GraphQlData(kontorHistorikk = innslag)))
    }

    private fun dto(
        kontorId: String = "0123",
        kontorNavn: String? = "NAV Oslo",
        kontorType: KontorTypeDto = KontorTypeDto.ARBEIDSOPPFOLGING,
        endretTidspunkt: String = "2024-05-01T10:15:30+02:00[Europe/Oslo]",
    ) = KontorhistorikkDto(
        kontorId = kontorId,
        kontorNavn = kontorNavn,
        kontorType = kontorType,
        endretTidspunkt = endretTidspunkt,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        KontorhistorikkHttpklient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
        )
    }

    /**
     * Vi sammenligner mot hele lista bevisst, slik at testen brekker dersom vi legger til (eller mister) felter på
     * [Kontorhistorikkinnslag] uten å tenke gjennom personvernkonsekvenser.
     */
    @Test
    fun `mapper alle innslag uten filtrering og POSTer til endepunktet`() {
        val transport = httpKlientMedInnslag(
            listOf(
                dto(
                    kontorId = "0123",
                    kontorNavn = "NAV Oslo",
                    kontorType = KontorTypeDto.ARBEIDSOPPFOLGING,
                    endretTidspunkt = "2024-05-01T10:15:30+02:00[Europe/Oslo]",
                ),
                dto(
                    kontorId = "0456",
                    kontorNavn = null,
                    kontorType = KontorTypeDto.ARENA,
                    endretTidspunkt = "2024-03-01T08:00:00+01:00[Europe/Oslo]",
                ),
                dto(
                    kontorId = "9999",
                    kontorNavn = "Skal IKKE filtreres ut",
                    kontorType = KontorTypeDto.GEOGRAFISK_TILKNYTNING,
                    endretTidspunkt = "2024-04-01T09:00:00+02:00[Europe/Oslo]",
                ),
            ),
        )

        runTest {
            val resultat = client(transport).hentKontorhistorikk(fnr).getOrNull().shouldNotBeNull()

            resultat.kontorhistorikk shouldBe Kontorhistorikk(
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
            )
            resultat.kall.httpStatus shouldBe 200
            resultat.httpKlientMetadata.shouldNotBeNull().statusCode shouldBe 200
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/graphql"
    }

    @Test
    fun `tom historikk gir tom liste`() {
        val transport = httpKlientMedInnslag(emptyList())

        runTest {
            client(transport).hentKontorhistorikk(fnr)
                .map { it.kontorhistorikk } shouldBe Kontorhistorikk(emptyList()).right()
        }
    }

    @Test
    fun `null kontorHistorikk i responsen gir tom liste`() {
        val transport = httpKlientMedInnslag(null)

        runTest {
            client(transport).hentKontorhistorikk(fnr)
                .map { it.kontorhistorikk } shouldBe Kontorhistorikk(emptyList()).right()
        }
    }

    @Test
    fun `null data uten errors gir tom liste`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(GraphQlResponse(data = null))
        }

        runTest {
            client(transport).hentKontorhistorikk(fnr)
                .map { it.kontorhistorikk } shouldBe Kontorhistorikk(emptyList()).right()
        }
    }

    @Test
    fun `non-200 fra tjenesten gir Left UventetHttpStatus`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 503, body = """{"message": "noe gikk galt"}""")
        }

        runTest {
            val feil = client(transport).hentKontorhistorikk(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.UventetHttpStatus>()
            feil.status shouldBe 503
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 503
            feil.kall.httpStatus shouldBe 503
        }
    }

    @Test
    fun `nettverksfeil gir Left KallFeilet`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.io.IOException("simulert nettverksfeil")) }

        runTest {
            val feil = client(transport).hentKontorhistorikk(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.NetworkError>()
            feil.kall.shouldNotBeNull().httpStatus shouldBe null
        }
    }

    @Test
    fun `GraphQL errors i respons gir Left GraphQlFeil`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                GraphQlResponse(
                    data = null,
                    errors = listOf(mapOf("message" to "noe gikk galt")),
                ),
            )
        }

        runTest {
            val feil = client(transport).hentKontorhistorikk(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.GraphQlFeil>()
            feil.httpKlientMetadata.shouldNotBeNull().statusCode shouldBe 200
            feil.httpKlientError shouldBe null
        }
    }

    @Test
    fun `tomt errors-felt behandles som suksess`() {
        val transport = FakeHttpTransport().apply {
            leggIKøJson(
                GraphQlResponse(
                    data = GraphQlData(kontorHistorikk = listOf(dto())),
                    errors = emptyList(),
                ),
            )
        }

        runTest {
            client(transport).hentKontorhistorikk(fnr).isRight() shouldBe true
        }
    }

    @Test
    fun `deserialiseringsfeil fra httpklient gir Left KallFeilet`() {
        // Modellerer f.eks. en ukjent kontorType-verdi i responsen: den ekte pipelinen klarer ikke å tolke body-en til [GraphQlResponse], og gir DeserializationError.
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 200, body = """{"data": {"kontorHistorikk": [{"kontorType": "EN_HELT_NY_TYPE"}]}}""")
        }

        runTest {
            val feil = client(transport).hentKontorhistorikk(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.DeserializationError>()
        }
    }

    @Test
    fun `mapper alle kjente kontorType-verdier til domene-enum`() {
        val transport = httpKlientMedInnslag(
            listOf(
                dto(kontorType = KontorTypeDto.ARBEIDSOPPFOLGING),
                dto(kontorType = KontorTypeDto.ARENA),
                dto(kontorType = KontorTypeDto.GEOGRAFISK_TILKNYTNING),
            ),
        )

        runTest {
            val resultat = client(transport).hentKontorhistorikk(fnr)
            resultat.getOrNull().shouldNotBeNull().kontorhistorikk.innslag.map { it.kontorType } shouldBe listOf(
                KontorType.ARBEIDSOPPFOLGING,
                KontorType.ARENA,
                KontorType.GEOGRAFISK_TILKNYTNING,
            )
        }
    }

    @Test
    fun `konverterer endretTidspunkt fra UTC til Europe-Oslo wall-clock`() {
        // Sommertid: 08-00 UTC = 10-00 Oslo.
        // Vintertid: 08-00 UTC = 09-00 Oslo.
        val transport = httpKlientMedInnslag(
            listOf(
                dto(kontorId = "sommer", endretTidspunkt = "2024-07-01T08:00:00Z[UTC]"),
                dto(kontorId = "vinter", endretTidspunkt = "2024-01-15T08:00:00Z[UTC]"),
            ),
        )

        runTest {
            val innslag = client(transport).hentKontorhistorikk(fnr).getOrNull().shouldNotBeNull().kontorhistorikk.innslag
            innslag.single { it.kontorId == "sommer" }.endretTidspunkt shouldBe
                LocalDateTime.parse("2024-07-01T10:00:00")
            innslag.single { it.kontorId == "vinter" }.endretTidspunkt shouldBe
                LocalDateTime.parse("2024-01-15T09:00:00")
        }
    }

    /**
     * Verifiserer at vi takler ISO-8601-varianter et "vanlig" API kan finne på å returnere.
     * Alle inputene her representerer det samme øyeblikket (08:00 UTC = 10:00 Oslo i sommertid), så vi forventer samme vegg-klokke-tidspunkt i Oslo uansett serialiseringsstil.
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
            val transport = httpKlientMedInnslag(listOf(dto(endretTidspunkt = input)))

            runTest {
                val faktisk = client(transport).hentKontorhistorikk(fnr).getOrNull().shouldNotBeNull()
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

    @Test
    fun `ugyldig endretTidspunkt-format gir Left KallFeilet med DeserializationError`() {
        val transport = httpKlientMedInnslag(listOf(dto(endretTidspunkt = "ikke-en-dato")))

        runTest {
            val feil = client(transport).hentKontorhistorikk(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
            // Mappingfeil pakkes som DeserializationError slik at throwable og metadata følger med til logging.
            feil.httpKlientError.shouldBeInstanceOf<HttpKlientError.DeserializationError>()
            feil.kall.shouldNotBeNull().httpStatus shouldBe 200
        }
    }

    /**
     * Et tidsstempel uten sone/offset (ren `LocalDateTime`) er tvetydig – vi vet ikke hva avsender mente.
     * Vi velger eksplisitt å feile heller enn å gjette på Europe/Oslo, slik at vi ikke ender opp med stille feil dersom APIet endrer kontrakt.
     */
    @Test
    fun `tidsstempel uten sone gir Left KallFeilet`() {
        val transport = httpKlientMedInnslag(listOf(dto(endretTidspunkt = "2024-07-01T10:00:00")))

        runTest {
            client(transport).hentKontorhistorikk(fnr).leftOrNull()
                .shouldNotBeNull()
                .shouldBeInstanceOf<KanIkkeHenteKontorhistorikk.KallFeilet>()
        }
    }
}
