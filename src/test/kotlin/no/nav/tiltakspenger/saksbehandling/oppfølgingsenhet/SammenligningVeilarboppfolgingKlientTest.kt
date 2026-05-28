package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
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
            kjørSammenligning = true,
        )

        runTest {
            klient.hentOppfolgingsenhet(Fnr.random()) shouldBe Navkontor(
                kontornummer = "0220",
                kontornavn = "Nav Asker",
            )
        }
    }

    @Test
    fun `kaller ikke ny klient når sammenligning er av`() {
        var nyKlientKalt = false
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient {
                nyKlientKalt = true
                KontorhistorikkFakeKlient.defaultHistorikk().right()
            },
            kjørSammenligning = false,
        )

        runTest {
            klient.hentOppfolgingsenhet(Fnr.random())
        }

        nyKlientKalt shouldBe false
    }

    @Test
    fun `Left fra ny klient påvirker ikke eksisterende svar`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient { KanIkkeHenteKontorhistorikk.KallFeilet.left() },
            kjørSammenligning = true,
        )

        runTest {
            klient.hentOppfolgingsenhet(Fnr.random()) shouldBe Navkontor(
                kontornummer = "0220",
                kontornavn = "Nav Asker",
            )
        }
    }

    @Test
    fun `exception fra ny klient påvirker ikke eksisterende svar`() {
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient { error("uventet feil i ny klient") },
            kjørSammenligning = true,
        )

        runTest {
            klient.hentOppfolgingsenhet(Fnr.random()) shouldBe Navkontor(
                kontornummer = "0220",
                kontornavn = "Nav Asker",
            )
        }
    }

    @Test
    fun `flere historikkinnslag - returnerer fortsatt eksisterende svar uavhengig`() {
        // Sanity-sjekk: dekoratøren skal håndtere flere innslag uten å påvirke svaret.
        val klient = SammenligningVeilarboppfolgingKlient(
            eksisterende = VeilarboppfolgingFakeKlient(),
            kontorhistorikkKlient = KontorhistorikkFakeKlient {
                Kontorhistorikk(
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
                ).right()
            },
            kjørSammenligning = true,
        )

        runTest {
            klient.hentOppfolgingsenhet(Fnr.random()) shouldBe Navkontor(
                kontornummer = "0220",
                kontornavn = "Nav Asker",
            )
        }
    }
}
