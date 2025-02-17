package no.nav.tiltakspenger.clients.utbetaling

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.vedtak.clients.utbetaling.toDTO
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingDTOTest {

    @Test
    fun `map utbetalingsvedtak til gyldig helved json`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val utbetalingsvedtak = ObjectMother.utbetalingsvedtak(
            fnr = fnr,
            id = id,
            saksnummer = saksnummer,
            opprettet = opprettet,
        )
        utbetalingsvedtak.toDTO(null).shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "0SZ5FBEE6YZG8S6",
              "iverksettingId": null,
              "personident": {
                "verdi": "09863149336"
              },
              "vedtak": {
                "vedtakstidspunkt": "2024-10-01T22:46:14.614465",
                "saksbehandlerId": "saksbehandler",
                "beslutterId": "beslutter",
                "utbetalinger": [
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2023-01-02",
                    "tilOgMedDato": "2023-01-06",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  }
                ]
              },
              "forrigeIverksetting": null
            }
            """.trimIndent(),
        )
    }
}
