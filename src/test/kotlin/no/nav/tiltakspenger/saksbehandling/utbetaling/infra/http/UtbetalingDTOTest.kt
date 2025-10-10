package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.meldekortBeregning
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdager
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class UtbetalingDTOTest {

    @Test
    fun `uten barnetillegg`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
        )
        meldekortVedtak.utbetaling.toUtbetalingRequestDTO(null).shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
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
                    "satstype": "DAGLIG",
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
                    "satstype": "DAGLIG",
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

    @Test
    fun `barnetillegg 1 barn hele perioden`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val periode = Periode(2.januar(2023), 15.januar(2023))
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            periode = periode,
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
            barnetilleggsPerioder = ObjectMother.barnetilleggsPerioder(
                periode = periode,
                antallBarn = AntallBarn(1),
            ),
        )
        meldekortVedtak.utbetaling.beregning.dager.map { it.beregningsdag }.forEach {
            withClue("Beregningsdag $it") {
                if (it!!.beløp > 0) it.beløpBarnetillegg shouldBe 52
            }
        }
        val actual = meldekortVedtak.utbetaling.toUtbetalingRequestDTO(null)
        actual.shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
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
                    "satstype": "DAGLIG",
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
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-02",
                    "tilOgMedDato": "2023-01-06",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
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

    @Test
    fun `barnetillegg 2 barn hele perioden`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val periode = Periode(2.januar(2023), 15.januar(2023))
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            periode = periode,
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
            barnetilleggsPerioder = ObjectMother.barnetilleggsPerioder(
                periode = periode,
                antallBarn = AntallBarn(2),
            ),
        )
        meldekortVedtak.utbetaling.beregning.dager.map { it.beregningsdag }.forEach {
            withClue("Beregningsdag $it") {
                if (it!!.beløp > 0) it.beløpBarnetillegg shouldBe 104
            }
        }
        val actual = meldekortVedtak.utbetaling.toUtbetalingRequestDTO(null)
        actual.shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
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
                    "satstype": "DAGLIG",
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
                    "beløp": 104,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-02",
                    "tilOgMedDato": "2023-01-06",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 104,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
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

    @Test
    fun `barnetillegg 1 barn første uke og 2 barn andre uke`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val periode = Periode(2.januar(2023), 15.januar(2023))
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            periode = periode,
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
            barnetilleggsPerioder = SammenhengendePeriodisering(
                PeriodeMedVerdi(
                    AntallBarn(1),
                    Periode(2.januar(2023), 8.januar(2023)),
                ),
                PeriodeMedVerdi(
                    AntallBarn(2),
                    Periode(9.januar(2023), 15.januar(2023)),
                ),
            ),
        )
        val actual = meldekortVedtak.utbetaling.toUtbetalingRequestDTO(null)
        actual.shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
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
                    "satstype": "DAGLIG",
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
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-02",
                    "tilOgMedDato": "2023-01-06",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 104,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
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

    @Test
    fun `0 barn på starten og slutten av meldeperioden`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val periode = Periode(2.januar(2023), 15.januar(2023))
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            periode = periode,
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
            barnetilleggsPerioder = SammenhengendePeriodisering(
                PeriodeMedVerdi(
                    AntallBarn(0),
                    Periode(2.januar(2023), 2.januar(2023)),
                ),
                PeriodeMedVerdi(
                    AntallBarn(1),
                    Periode(3.januar(2023), 12.januar(2023)),
                ),
                PeriodeMedVerdi(
                    AntallBarn(0),
                    Periode(13.januar(2023), 15.januar(2023)),
                ),
            ),
        )
        val actual = meldekortVedtak.utbetaling.toUtbetalingRequestDTO(null)
        actual.shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
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
                    "satstype": "DAGLIG",
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
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-03",
                    "tilOgMedDato": "2023-01-06",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-12",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
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

    @Test
    fun `0 barn midt i en uke`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val periode = Periode(2.januar(2023), 15.januar(2023))
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            periode = periode,
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
            barnetilleggsPerioder = SammenhengendePeriodisering(
                PeriodeMedVerdi(
                    AntallBarn(1),
                    Periode(2.januar(2023), 3.januar(2023)),
                ),
                PeriodeMedVerdi(
                    AntallBarn(0),
                    Periode(4.januar(2023), 4.januar(2023)),
                ),
                PeriodeMedVerdi(
                    AntallBarn(1),
                    Periode(5.januar(2023), 6.januar(2023)),
                ),
                PeriodeMedVerdi(
                    AntallBarn(1),
                    Periode(7.januar(2023), 15.januar(2023)),
                ),
            ),
        )
        val actual = meldekortVedtak.utbetaling.toUtbetalingRequestDTO(null)
        actual.shouldEqualJson(
            """
            {
              "sakId": "202410011001",
              "behandlingId": "Z73MKA2083E4WDE",
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
                    "satstype": "DAGLIG",
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
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-02",
                    "tilOgMedDato": "2023-01-03",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-05",
                    "tilOgMedDato": "2023-01-06",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 268,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2023-01-02/2023-01-15"
                    }
                  },
                  {
                    "beløp": 52,
                    "satstype": "DAGLIG",
                    "fraOgMedDato": "2023-01-09",
                    "tilOgMedDato": "2023-01-13",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": true,
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

    @Test
    fun `Skal feile ved utbetaling på helgedager`() {
        val fnr = Fnr.fromString("09863149336")
        val id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6")
        val utbetalingId = UtbetalingId.fromString("utbetaling_01JK6295T9WZ73MKA2083E4WDE")
        val saksnummer = Saksnummer("202410011001")
        val opprettet = LocalDateTime.parse("2024-10-01T22:46:14.614465")
        val periode = Periode(2.januar(2023), 15.januar(2023))
        val meldekortVedtak = ObjectMother.meldekortVedtak(
            periode = periode,
            fnr = fnr,
            id = id,
            utbetalingId = utbetalingId,
            saksnummer = saksnummer,
            opprettet = opprettet,
        )

        val utbetalingMedHelger = meldekortVedtak.utbetaling.copy(
            beregning = meldekortBeregning(
                beregningDager = tiltaksdager(
                    startDato = periode.fraOgMed,
                    meldekortId = meldekortVedtak.meldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 5,
                ) + tiltaksdager(
                    startDato = periode.fraOgMed.plusDays(5),
                    meldekortId = meldekortVedtak.meldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 2,
                ) + tiltaksdager(
                    startDato = periode.fraOgMed.plusDays(7),
                    meldekortId = meldekortVedtak.meldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 5,
                ) + tiltaksdager(
                    startDato = periode.fraOgMed.plusDays(12),
                    meldekortId = meldekortVedtak.meldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 2,
                ),
            ),
        )

        shouldThrow<IllegalArgumentException> {
            utbetalingMedHelger.toUtbetalingRequestDTO(null)
        }.message.shouldBe("Helgedager kan ikke ha et beregnet beløp, ettersom det ikke vil bli utbetalt - dato: 2023-01-07")
    }
}
