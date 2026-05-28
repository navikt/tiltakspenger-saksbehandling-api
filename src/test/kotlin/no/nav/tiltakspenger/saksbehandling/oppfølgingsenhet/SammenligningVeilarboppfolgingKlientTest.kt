package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.KontorType
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Kontorhistorikk.Kontorhistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.KontorhistorikkFakeKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.VeilarboppfolgingFakeKlient
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SammenligningVeilarboppfolgingKlientTest {

    @Test
    fun `returnerer alltid resultat fra eksisterende klient`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient(),
        )

        runTest {
            val resultat = klient.hentOppfolgingsenhet(Fnr.random()).getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
            resultat.brukteKlient shouldBe BruktNavkontorKlient.VEILARBOPPFOLGING
            resultat.veilarboppfolgingKall.shouldNotBeNull()
            resultat.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `kaller alltid ny klient`() {
        var nyKlientKalt = false
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient { fnr ->
                nyKlientKalt = true
                KontorhistorikkMedMetadata(
                    kontorhistorikk = KontorhistorikkFakeKlient.defaultHistorikk(),
                    kall = KontorhistorikkFakeKlient.defaultKall(fnr),
                ).right()
            },
        )

        runTest {
            klient.hentOppfolgingsenhet(Fnr.random())
        }

        nyKlientKalt shouldBe true
    }

    @Test
    fun `Left fra ny klient påvirker ikke eksisterende svar`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient { KanIkkeHenteKontorhistorikk.KallFeilet().left() },
        )

        runTest {
            val resultat = klient.hentOppfolgingsenhet(Fnr.random()).getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
            resultat.brukteKlient shouldBe BruktNavkontorKlient.VEILARBOPPFOLGING
        }
    }

    @Test
    fun `exception fra ny klient påvirker ikke eksisterende svar`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient { error("uventet feil i ny klient") },
        )

        runTest {
            val resultat = klient.hentOppfolgingsenhet(Fnr.random()).getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
        }
    }

    @Test
    fun `Left fra eksisterende klient propageres med kontorhistorikkKall vedlagt`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient {
                KanIkkeHenteOppfølgingsenhet.KallFeilet(
                    veilarboppfolgingKall = Klientkall(request = "{}", response = null, httpStatus = null),
                ).left()
            },
            kontorhistorikkKlient = KontorhistorikkFakeKlient(),
        )

        runTest {
            val feil = klient.hentOppfolgingsenhet(Fnr.random()).leftOrNull()!!
            feil.shouldBeInstanceOf<KanIkkeHenteOppfølgingsenhet.KallFeilet>()
            feil.veilarboppfolgingKall.shouldNotBeNull()
            feil.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `faller tilbake på ny klient når gammel klient mangler oppfolgingsenhet`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient {
                KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet(
                    veilarboppfolgingKall = Klientkall(request = "{}", response = "{}", httpStatus = 200),
                ).left()
            },
            kontorhistorikkKlient = KontorhistorikkFakeKlient { fnr ->
                KontorhistorikkMedMetadata(
                    kontorhistorikk = Kontorhistorikk(
                        listOf(
                            Kontorhistorikkinnslag(
                                kontorId = "1234",
                                kontorNavn = "Nav Ny",
                                kontorType = KontorType.ARBEIDSOPPFOLGING,
                                endretTidspunkt = LocalDateTime.parse("2024-05-01T10:00:00"),
                            ),
                        ),
                    ),
                    kall = KontorhistorikkFakeKlient.defaultKall(fnr),
                ).right()
            },
        )

        runTest {
            val resultat = klient.hentOppfolgingsenhet(Fnr.random()).getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "1234", kontornavn = "Nav Ny")
            resultat.brukteKlient shouldBe BruktNavkontorKlient.KONTORHISTORIKK
            resultat.veilarboppfolgingKall.shouldNotBeNull()
            resultat.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `propagerer ManglerOppfolgingsenhet når både gammel og ny mangler kontor`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient {
                KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet(
                    veilarboppfolgingKall = Klientkall(request = "{}", response = "{}", httpStatus = 200),
                ).left()
            },
            kontorhistorikkKlient = KontorhistorikkFakeKlient { fnr ->
                KontorhistorikkMedMetadata(
                    kontorhistorikk = Kontorhistorikk(emptyList()),
                    kall = KontorhistorikkFakeKlient.defaultKall(fnr),
                ).right()
            },
        )

        runTest {
            val feil = klient.hentOppfolgingsenhet(Fnr.random()).leftOrNull()!!
            feil.shouldBeInstanceOf<KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet>()
            feil.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `andre Left enn ManglerOppfolgingsenhet faller IKKE tilbake på ny klient`() {
        // Vi vil ikke skjule kall-/HTTP-/tjenestefeil ved å bytte til ny klient. Bare nullsvar (mangler).
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient {
                KanIkkeHenteOppfølgingsenhet.UventetHttpStatus(
                    status = 503,
                    veilarboppfolgingKall = Klientkall(request = "{}", response = "fail", httpStatus = 503),
                ).left()
            },
            kontorhistorikkKlient = KontorhistorikkFakeKlient(),
        )

        runTest {
            val feil = klient.hentOppfolgingsenhet(Fnr.random()).leftOrNull()!!
            feil.shouldBeInstanceOf<KanIkkeHenteOppfølgingsenhet.UventetHttpStatus>()
            feil.kontorhistorikkKall.shouldNotBeNull()
        }
    }

    @Test
    fun `flere historikkinnslag - returnerer fortsatt eksisterende svar uavhengig`() {
        // Sanity-sjekk: dekoratøren skal håndtere flere innslag uten å påvirke svaret.
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient { fnr ->
                KontorhistorikkMedMetadata(
                    kontorhistorikk = Kontorhistorikk(
                        listOf(
                            Kontorhistorikkinnslag(
                                kontorId = "0220",
                                kontorNavn = "Nav Asker",
                                kontorType = KontorType.ARENA,
                                endretTidspunkt = LocalDateTime.parse("2024-05-01T10:00:00"),
                            ),
                            Kontorhistorikkinnslag(
                                kontorId = "9999",
                                kontorNavn = "Annet kontor",
                                kontorType = KontorType.GEOGRAFISK_TILKNYTNING,
                                endretTidspunkt = LocalDateTime.parse("2023-01-01T10:00:00"),
                            ),
                        ),
                    ),
                    kall = KontorhistorikkFakeKlient.defaultKall(fnr),
                ).right()
            },
        )

        runTest {
            val resultat = klient.hentOppfolgingsenhet(Fnr.random()).getOrNull()!!
            resultat.navkontor shouldBe Navkontor(kontornummer = "0220", kontornavn = "Nav Asker")
        }
    }
}
