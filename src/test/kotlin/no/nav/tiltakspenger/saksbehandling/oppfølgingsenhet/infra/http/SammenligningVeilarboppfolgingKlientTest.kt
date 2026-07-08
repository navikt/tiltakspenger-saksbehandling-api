package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlientFake
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteNavkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Klientene er konkrete klasser uten interface, så vi faker på HTTP-nivå med [HttpKlientFake].
 * Testene dekker dermed også JSON-mappingen i begge klientene, ikke bare valg-/logglogikken.
 */
internal class SammenligningVeilarboppfolgingKlientTest {

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun klient(
        veilarbHttp: HttpKlientFake,
        kontorhistorikkHttp: HttpKlientFake,
    ) = SammenligningVeilarboppfolgingKlient(
        eksisterende = VeilarboppfolgingHttpClient(
            baseUrl = "http://veilarboppfolging.test",
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
            httpKlient = veilarbHttp,
        ),
        kontorhistorikkKlient = KontorhistorikkHttpklient(
            baseUrl = "http://ao-oppfolgingskontor.test",
            authTokenProvider = authTokenProvider,
            clock = ObjectMother.clock,
            httpKlient = kontorhistorikkHttp,
        ),
    )

    private fun veilarbHttpMedEnhet(
        navn: String = "Nav Asker",
        enhetId: String = "0220",
    ) = HttpKlientFake().apply {
        enqueueResponse(
            body = Response(
                oppfolgingsenhet = Oppfolgingsenhet(navn = navn, enhetId = enhetId),
                veilederId = null,
                formidlingsgruppe = null,
                servicegruppe = null,
                hovedmaalkode = null,
            ),
        )
    }

    private fun veilarbHttpUtenEnhet() = HttpKlientFake().apply {
        enqueueResponse(
            body = Response(
                oppfolgingsenhet = null,
                veilederId = null,
                formidlingsgruppe = null,
                servicegruppe = null,
                hovedmaalkode = null,
            ),
        )
    }

    private fun kontorhistorikkHttpMedInnslag(vararg innslag: KontorhistorikkDto) = HttpKlientFake().apply {
        enqueueResponse(body = GraphQlResponse(data = GraphQlData(kontorHistorikk = innslag.toList())))
    }

    private fun dto(
        kontorId: String = "0220",
        kontorNavn: String? = "Nav Asker",
        kontorType: KontorTypeDto = KontorTypeDto.ARENA,
        endretTidspunkt: String = "2024-05-01T10:00:00+02:00[Europe/Oslo]",
    ) = KontorhistorikkDto(
        kontorId = kontorId,
        kontorNavn = kontorNavn,
        kontorType = kontorType,
        endretTidspunkt = endretTidspunkt,
    )

    @Test
    fun `returnerer alltid resultat fra eksisterende klient`() {
        val klient = klient(
            veilarbHttp = veilarbHttpMedEnhet(),
            kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(dto()),
        )

        runTest {
            val resultat = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
            resultat.brukteKlient shouldBe BruktNavkontorKlient.VEILARBOPPFOLGING
            resultat.veilarboppfolgingKall.shouldNotBeNull()
            resultat.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `kaller alltid ny klient`() {
        val kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(dto())
        val klient = klient(
            veilarbHttp = veilarbHttpMedEnhet(),
            kontorhistorikkHttp = kontorhistorikkHttp,
        )

        runTest {
            klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123")
        }

        kontorhistorikkHttp.requests.size shouldBe 1
    }

    @Test
    fun `Left fra ny klient påvirker ikke eksisterende svar`() {
        val klient = klient(
            veilarbHttp = veilarbHttpMedEnhet(),
            kontorhistorikkHttp = HttpKlientFake().apply { enqueueNetworkError() },
        )

        runTest {
            val resultat = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
            resultat.brukteKlient shouldBe BruktNavkontorKlient.VEILARBOPPFOLGING
        }
    }

    @Test
    fun `exception fra ny klient påvirker ikke eksisterende svar`() {
        val klient = klient(
            veilarbHttp = veilarbHttpMedEnhet(),
            kontorhistorikkHttp = HttpKlientFake().apply { enqueue { error("uventet feil i ny klient") } },
        )

        runTest {
            val resultat = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
        }
    }

    @Test
    fun `Left fra eksisterende klient propageres med kontorhistorikkKall vedlagt`() {
        val klient = klient(
            veilarbHttp = HttpKlientFake().apply { enqueueNetworkError() },
            kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(dto()),
        )

        runTest {
            val feil = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").leftOrNull()!!
            feil.shouldBeInstanceOf<KanIkkeHenteNavkontor.KallFeilet>()
            feil.veilarboppfolgingKall.shouldNotBeNull()
            feil.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `faller tilbake på ny klient når gammel klient mangler oppfolgingsenhet`() {
        val klient = klient(
            veilarbHttp = veilarbHttpUtenEnhet(),
            kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(
                dto(
                    kontorId = "1234",
                    kontorNavn = "Nav Ny",
                    kontorType = KontorTypeDto.ARBEIDSOPPFOLGING,
                ),
            ),
        )

        runTest {
            val resultat = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "1234", kontornavn = "Nav Ny")
            resultat.brukteKlient shouldBe BruktNavkontorKlient.KONTORHISTORIKK
            resultat.veilarboppfolgingKall.shouldNotBeNull()
            resultat.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `propagerer ManglerOppfolgingsenhet når både gammel og ny mangler kontor`() {
        val klient = klient(
            veilarbHttp = veilarbHttpUtenEnhet(),
            kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(),
        )

        runTest {
            val feil = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").leftOrNull()!!
            feil.shouldBeInstanceOf<KanIkkeHenteNavkontor.ManglerOppfolgingsenhet>()
            feil.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `andre Left enn ManglerOppfolgingsenhet faller IKKE tilbake på ny klient`() {
        // Vi vil ikke skjule kall-/HTTP-/tjenestefeil ved å bytte til ny klient. Bare nullsvar (mangler).
        val klient = klient(
            veilarbHttp = HttpKlientFake().apply { enqueueUventetStatus(statusCode = 503, body = "fail") },
            kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(dto()),
        )

        runTest {
            val feil = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").leftOrNull()!!
            feil.shouldBeInstanceOf<KanIkkeHenteNavkontor.UventetHttpStatus>()
            feil.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `flere historikkinnslag - returnerer fortsatt eksisterende svar uavhengig`() {
        // Sanity-sjekk: sammenligningen skal håndtere flere innslag uten å påvirke svaret.
        val klient = klient(
            veilarbHttp = veilarbHttpMedEnhet(),
            kontorhistorikkHttp = kontorhistorikkHttpMedInnslag(
                dto(
                    kontorId = "0220",
                    kontorNavn = "Nav Asker",
                    kontorType = KontorTypeDto.ARENA,
                    endretTidspunkt = "2024-05-01T10:00:00+02:00[Europe/Oslo]",
                ),
                dto(
                    kontorId = "9999",
                    kontorNavn = "Annet kontor",
                    kontorType = KontorTypeDto.GEOGRAFISK_TILKNYTNING,
                    endretTidspunkt = "2023-01-01T10:00:00+01:00[Europe/Oslo]",
                ),
            ),
        )

        runTest {
            val resultat = klient.hentNavkontor(Fnr.random(), loggkontekst = "sakId: 123").getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
        }
    }
}
