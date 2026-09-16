package no.nav.tiltakspenger.saksbehandling.søknad

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Historikken har to invarianter: den er kronologisk, og den alternerer fra avbrutt til gjenåpnet.
 * Vakten står i `init`, så den treffer både prodstien og lesingen fra databasen.
 */
class SøknadshendelserTest {

    private fun avbrutt(tidspunkt: LocalDateTime) = Søknadshendelse.Avbrutt(
        tidspunkt = tidspunkt,
        utførtAv = "Z123456",
        begrunnelse = "duplikat".toNonBlankString(),
    )

    private fun gjenåpnet(tidspunkt: LocalDateTime) = Søknadshendelse.Gjenåpnet(
        tidspunkt = tidspunkt,
        utførtAv = "Z654321",
        begrunnelse = "avbrutt ved en feil".toNonBlankString(),
    )

    @Nested
    inner class LovligeHistorikker {
        @Test
        fun `en tom historikk er lovlig`() {
            val hendelser = Søknadshendelser.empty()

            hendelser.toList() shouldBe emptyList()
            hendelser.erAvbrutt shouldBe false
            hendelser.erGjenåpnet shouldBe false
        }

        @Test
        fun `et enslig avbrudd er lovlig`() {
            Søknadshendelser(listOf(avbrutt(1.januar(2025).atTime(12, 0)))).erAvbrutt shouldBe true
        }

        @Test
        fun `flere runder med avbrudd og gjenåpning er lovlig`() {
            val hendelser = Søknadshendelser(
                listOf(
                    avbrutt(1.januar(2025).atTime(12, 0)),
                    gjenåpnet(2.januar(2025).atTime(13, 30)),
                    avbrutt(3.januar(2025).atTime(9, 15)),
                    gjenåpnet(4.januar(2025).atTime(10, 45)),
                ),
            )

            hendelser.size shouldBe 4
            hendelser.erGjenåpnet shouldBe true
        }

        @Test
        fun `plus bygger historikken videre uten å bryte invariantene`() {
            val avbrutt = Søknadshendelser.fromAvbrutt(
                tidspunkt = 1.januar(2025).atTime(12, 0),
                utførtAv = "Z123456",
                begrunnelse = "duplikat".toNonBlankString(),
            )

            shouldNotThrowAny { avbrutt + gjenåpnet(2.januar(2025).atTime(13, 30)) }
        }
    }

    @Nested
    inner class Kronologi {
        @Test
        fun `en hendelse som ligger før den forrige avvises`() {
            shouldThrowWithMessage<IllegalArgumentException>(
                "Søknadshendelsene må være i stigende kronologisk rekkefølge, men 2025-01-01T12:00 kom etter 2025-01-03T09:15.",
            ) {
                Søknadshendelser(
                    listOf(
                        avbrutt(3.januar(2025).atTime(9, 15)),
                        gjenåpnet(1.januar(2025).atTime(12, 0)),
                    ),
                )
            }
        }

        @Test
        fun `to hendelser på samme tidspunkt avvises`() {
            val tidspunkt = 1.januar(2025).atTime(12, 0)

            shouldThrowWithMessage<IllegalArgumentException>(
                "Søknadshendelsene må være i stigende kronologisk rekkefølge, men 2025-01-01T12:00 kom etter 2025-01-01T12:00.",
            ) {
                Søknadshendelser(listOf(avbrutt(tidspunkt), gjenåpnet(tidspunkt)))
            }
        }

        @Test
        fun `kronologien sjekkes gjennom hele historikken, ikke bare i starten`() {
            shouldThrowWithMessage<IllegalArgumentException>(
                "Søknadshendelsene må være i stigende kronologisk rekkefølge, men 2025-01-02T13:30 kom etter 2025-01-03T09:15.",
            ) {
                Søknadshendelser(
                    listOf(
                        avbrutt(1.januar(2025).atTime(12, 0)),
                        gjenåpnet(3.januar(2025).atTime(9, 15)),
                        avbrutt(2.januar(2025).atTime(13, 30)),
                    ),
                )
            }
        }
    }

    @Nested
    inner class Alternering {
        @Test
        fun `en historikk som starter med gjenåpnet avvises`() {
            shouldThrowWithMessage<IllegalArgumentException>(
                "Søknadshendelsene må alternere mellom avbrutt og gjenåpnet, men hendelse nr. 1 var gjenåpnet.",
            ) {
                Søknadshendelser(listOf(gjenåpnet(1.januar(2025).atTime(12, 0))))
            }
        }

        @Test
        fun `to avbrudd på rad avvises`() {
            shouldThrowWithMessage<IllegalArgumentException>(
                "Søknadshendelsene må alternere mellom avbrutt og gjenåpnet, men hendelse nr. 2 var avbrutt.",
            ) {
                Søknadshendelser(
                    listOf(
                        avbrutt(1.januar(2025).atTime(12, 0)),
                        avbrutt(2.januar(2025).atTime(13, 30)),
                    ),
                )
            }
        }

        @Test
        fun `to gjenåpninger på rad avvises`() {
            shouldThrowWithMessage<IllegalArgumentException>(
                "Søknadshendelsene må alternere mellom avbrutt og gjenåpnet, men hendelse nr. 3 var gjenåpnet.",
            ) {
                Søknadshendelser(
                    listOf(
                        avbrutt(1.januar(2025).atTime(12, 0)),
                        gjenåpnet(2.januar(2025).atTime(13, 30)),
                        gjenåpnet(3.januar(2025).atTime(9, 15)),
                    ),
                )
            }
        }
    }
}
