package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.fixedClock
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class TiltaksdeltakelseHttpKlientTest {
    private val baseUrl = "http://tiltakspenger-tiltak.test"
    private val fnr = Fnr.random()

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun client(transport: FakeHttpTransport) = TiltaksdeltakelseHttpKlient(
        baseUrl = baseUrl,
        authTokenProvider = authTokenProvider,
        clock = fixedClock,
        transport = transport,
    )

    private fun tiltakshistorikkDTO(
        arenaKode: TiltakResponsDTO.TiltakTypeDTO = TiltakResponsDTO.TiltakTypeDTO.ARBFORB,
        deltakelseFom: LocalDate? = 1.januar(2023),
        deltakelseTom: LocalDate? = 31.mars(2023),
        deltakelseStatus: TiltakResponsDTO.DeltakerStatusDTO = TiltakResponsDTO.DeltakerStatusDTO.HAR_SLUTTET,
    ) = TiltakshistorikkDTO(
        id = UUID.randomUUID().toString(),
        gjennomforing = TiltakshistorikkDTO.GjennomforingDTO(
            id = UUID.randomUUID().toString(),
            visningsnavn = "Arbeidsforberedende trening hos Arrangør i Gjøvik AS",
            arenaKode = arenaKode,
            arrangornavn = "Arrangør i Gjøvik AS",
            typeNavn = "Arbeidsforberedende trening",
            deltidsprosent = 100.0,
        ),
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        deltakelseStatus = deltakelseStatus,
        antallDagerPerUke = 5.0f,
        deltakelseProsent = 100.0f,
        kilde = TiltakshistorikkDTO.Kilde.KOMET,
    )

    @Test
    fun `bygger default HttpKlient når httpKlient ikke sendes inn`() {
        TiltaksdeltakelseHttpKlient(
            baseUrl = baseUrl,
            authTokenProvider = authTokenProvider,
            clock = fixedClock,
        )
    }

    @Test
    fun `hentTiltaksdeltakelser - filtrerer vekk tiltak uten rett og tiltak uten datoer`() {
        val relevantDeltakelse = tiltakshistorikkDTO()
        val utenRett = tiltakshistorikkDTO(arenaKode = TiltakResponsDTO.TiltakTypeDTO.MIDLONTIL)
        val utenDatoerOgIkkeVenterPåOppstart = tiltakshistorikkDTO(deltakelseFom = null, deltakelseTom = null)
        val transport = FakeHttpTransport().apply {
            leggIKøJson(listOf(relevantDeltakelse, utenRett, utenDatoerOgIkkeVenterPåOppstart))
        }
        val correlationId = CorrelationId.generate()

        runTest {
            val tiltaksdeltakelser = client(transport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = correlationId,
            ).getOrNull().shouldNotBeNull()

            tiltaksdeltakelser.value.map { it.eksternDeltakelseId } shouldBe listOf(relevantDeltakelse.id)
        }

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/azure/tiltakshistorikk"
        kall.request.headers().allValues("Nav-Call-Id") shouldBe listOf(correlationId.value)
    }

    @Test
    fun `hentTiltaksdeltakelser - tiltak uten datoer som venter på oppstart beholdes`() {
        val venterPåOppstart = tiltakshistorikkDTO(
            deltakelseFom = null,
            deltakelseTom = null,
            deltakelseStatus = TiltakResponsDTO.DeltakerStatusDTO.VENTER_PA_OPPSTART,
        )
        val transport = FakeHttpTransport().apply { leggIKøJson(listOf(venterPåOppstart)) }

        runTest {
            val tiltaksdeltakelser = client(transport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = CorrelationId.generate(),
            ).getOrNull().shouldNotBeNull()

            tiltaksdeltakelser.value.map { it.eksternDeltakelseId } shouldBe listOf(venterPåOppstart.id)
        }
    }

    @Test
    fun `hentTiltaksdeltakelser - feilstatus gir Left med HttpKlientError`() {
        val transport = FakeHttpTransport().apply { leggIKøStatus(statusCode = 500, body = "feil", contentType = "text/plain") }

        runTest {
            val feil = client(transport).hentTiltaksdeltakelser(
                fnr = fnr,
                tiltaksdeltakelserDetErSøktTiltakspengerFor = TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(),
                correlationId = CorrelationId.generate(),
            ).leftOrNull().shouldNotBeNull()

            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
        }
    }

    @Test
    fun `hentTiltaksdeltakelserMedArrangørnavn - maskerer arrangørnavn ved adressebeskyttelse`() {
        val deltakelse = tiltakshistorikkDTO()
        val transport = FakeHttpTransport().apply { leggIKøJson(listOf(deltakelse)) }

        runTest {
            val tiltaksdeltakelser = client(transport).hentTiltaksdeltakelserMedArrangørnavn(
                fnr = fnr,
                harAdressebeskyttelse = true,
                correlationId = CorrelationId.generate(),
            ).getOrNull().shouldNotBeNull()

            tiltaksdeltakelser.single().visningsnavn shouldBe deltakelse.gjennomforing.typeNavn
        }
    }

    @Test
    fun `TiltakRequestDTO maskerer fnr i toString`() {
        TiltakRequestDTO(ident = fnr.verdi).toString() shouldBe "TiltakRequestDTO(ident=*****)"
    }

    @Test
    fun `hentTiltaksdeltakelserMedArrangørnavn - timeout gir Left`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(java.net.http.HttpTimeoutException("simulert timeout")) }

        runTest {
            val feil = client(transport).hentTiltaksdeltakelserMedArrangørnavn(
                fnr = fnr,
                harAdressebeskyttelse = false,
                correlationId = CorrelationId.generate(),
            ).leftOrNull().shouldNotBeNull()

            feil.shouldBeInstanceOf<HttpKlientError.Timeout>()
        }
    }
}
