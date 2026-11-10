package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilUtbetalingerDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toUtbetalingRequestDTO
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HelvedUtbetalingDTOTest {
    private val fnr = ObjectMother.gyldigFnr()
    private val saksnummer = Saksnummer("202410011001")

    private val førsteVedtak = ObjectMother.meldekortvedtak(
        fnr = fnr,
        id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6"),
        utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE"),
        saksnummer = saksnummer,
        opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465"),
    )

    private val andreVedtak = ObjectMother.meldekortvedtak(
        periode = Periode(16.januar(2023), 29.januar(2023)),
        fnr = fnr,
        saksnummer = saksnummer,
    )

    @Test
    fun `lager utbetaling til Hel Ved med en periode per uke`() {
        førsteVedtak.utbetaling.tilHelvedUtbetalingJson(null).shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
              "dryrun": false,
              "personident": "${fnr.verdi}",
              "vedtakstidspunkt": "2024-10-01T22:46:14.614465",
              "perioder": [
                {
                  "meldeperiode": "2023-01-02/2023-01-15",
                  "fom": "2023-01-02",
                  "tom": "2023-01-06",
                  "betalendeEnhet": "0220",
                  "barnetillegg": false,
                  "beløp": 268,
                  "stønad": "GRUPPE_AMO"
                },
                {
                  "meldeperiode": "2023-01-02/2023-01-15",
                  "fom": "2023-01-09",
                  "tom": "2023-01-13",
                  "betalendeEnhet": "0220",
                  "barnetillegg": false,
                  "beløp": 268,
                  "stønad": "GRUPPE_AMO"
                }
              ],
              "saksbehandler": "saksbehandler",
              "beslutter": "beslutter"
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `forrige utbetaling leses likt fra versjon 2 og versjon 3`() {
        val forrigeV2 = førsteVedtak.utbetaling.toUtbetalingRequestDTO(null)
        val forrigeV3 = førsteVedtak.utbetaling.tilHelvedUtbetalingJson(null)

        val andreUtbetaling = andreVedtak.utbetaling
        val fraV2 = andreUtbetaling.beregning.tilUtbetalingerDTO(andreUtbetaling.brukerNavkontor, forrigeV2)
        val fraV3 = andreUtbetaling.beregning.tilUtbetalingerDTO(andreUtbetaling.brukerNavkontor, forrigeV3)

        fraV3 shouldBe fraV2
        fraV3.map { it.stønadsdata.meldekortId }.distinct() shouldBe listOf(
            "2023-01-02/2023-01-15",
            "2023-01-16/2023-01-29",
        )
    }
}
