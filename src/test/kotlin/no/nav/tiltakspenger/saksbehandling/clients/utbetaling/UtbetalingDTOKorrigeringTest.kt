package no.nav.tiltakspenger.saksbehandling.clients.utbetaling

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.toDTO
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class UtbetalingDTOKorrigeringTest {
    val fnr = Fnr.fromString("09863149336")
    val sakId = SakId.random()
    val saksnummer = Saksnummer("202410011001")
    val førstePeriode = Periode(
        LocalDate.of(2025, 1, 6),
        LocalDate.of(2025, 1, 19),
    )

    private fun lagUtbetalingsVedtak(
        id: VedtakId,
        meldekortId: MeldekortId,
        periode: Periode,
        opprettet: LocalDateTime,
        dager: NonEmptyList<MeldeperiodeBeregningDag.Utfylt>,
        beregninger: NonEmptyList<MeldeperiodeBeregning> = nonEmptyListOf(
            MeldeperiodeBeregning(
                kjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
                meldekortId = meldekortId,
                beregnet = opprettet,
                sakId = sakId,
                dager = dager,
            ),
        ),
    ) = ObjectMother.utbetalingsvedtak(
        fnr = fnr,
        saksnummer = saksnummer,
        id = id,
        opprettet = opprettet,
        meldekortBehandling = ObjectMother.meldekortBehandlet(
            id = meldekortId,
            periode = periode,
            meldekortperiodeBeregning = ObjectMother.meldekortBeregning(
                startDato = periode.fraOgMed,
                meldekortId = meldekortId,
                dager = dager,
                beregninger = beregninger,
            ),
        ),
    )

    @Test
    fun `Skal korrigere ett meldekort`() {
        val førsteMeldekortId = MeldekortId.random()
        val korrigertMeldekortId = MeldekortId.random()

        val førsteUtbetalingsvedtak = lagUtbetalingsVedtak(
            id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6"),
            meldekortId = førsteMeldekortId,
            periode = førstePeriode,
            opprettet = LocalDateTime.parse("2025-01-01T00:00:00.000001"),
            dager = ObjectMother.maksAntallDeltattTiltaksdagerIMeldekortperiode(
                startDato = førstePeriode.fraOgMed,
                meldekortId = førsteMeldekortId,
                tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
            ),
        )

        val korrigertUtbetalingsvedtak = lagUtbetalingsVedtak(
            id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7"),
            meldekortId = korrigertMeldekortId,
            opprettet = LocalDateTime.parse("2025-01-19T00:00:00.000001"),
            periode = førstePeriode,
            dager = ObjectMother.tiltaksdager(
                startDato = førstePeriode.fraOgMed,
                meldekortId = korrigertMeldekortId,
                tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                antallDager = 4,
            ).plus(
                ObjectMother.ikkeTiltaksdager(
                    startDato = førstePeriode.fraOgMed.plusDays(4),
                    meldekortId = korrigertMeldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 3,
                ),
            ).plus(
                ObjectMother.tiltaksdager(
                    startDato = førstePeriode.fraOgMed.plusDays(7),
                    meldekortId = korrigertMeldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 4,
                ),
            ).plus(
                ObjectMother.ikkeTiltaksdager(
                    startDato = førstePeriode.fraOgMed.plusDays(11),
                    meldekortId = korrigertMeldekortId,
                    tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                    antallDager = 3,
                ),
            ).toNonEmptyListOrNull()!!,
        )

        val førsteJson = førsteUtbetalingsvedtak.toDTO(null)

        @Language("JSON")
        val forventetKorrigertJson =
            """
            {
              "sakId": "202410011001",
              "behandlingId": "0SZ5FBEE6YZG8S7",
              "iverksettingId": null,
              "personident": {
                "verdi": "09863149336"
              },
              "vedtak": {
                "vedtakstidspunkt": "2025-01-19T00:00:00.000001",
                "saksbehandlerId": "saksbehandler",
                "beslutterId": "beslutter",
                "utbetalinger": [
                  {
                    "beløp": 298,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2025-01-06",
                    "tilOgMedDato": "2025-01-09",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2025-01-06/2025-01-19"
                    }
                  },
                  {
                    "beløp": 298,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2025-01-13",
                    "tilOgMedDato": "2025-01-16",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2025-01-06/2025-01-19"
                    }
                  }
                ]
              },
              "forrigeIverksetting": null
            }
            """.trimIndent()

        korrigertUtbetalingsvedtak.toDTO(førsteJson).shouldEqualJson(forventetKorrigertJson)
    }

    @Test
    fun `Skal korrigere det første av to meldekort`() {
        val førsteMeldekortId = MeldekortId.random()
        val andreMeldekortId = MeldekortId.random()
        val korrigertMeldekortId = MeldekortId.random()

        val andrePeriode = førstePeriode.plus14Dager()
        val andrePeriodeDager = ObjectMother.maksAntallDeltattTiltaksdagerIMeldekortperiode(
            startDato = andrePeriode.fraOgMed,
            meldekortId = andreMeldekortId,
            tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
        )
        val korrigerteDager = ObjectMother.tiltaksdager(
            startDato = førstePeriode.fraOgMed,
            meldekortId = korrigertMeldekortId,
            tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
            antallDager = 4,
        ).plus(
            ObjectMother.ikkeTiltaksdager(
                startDato = førstePeriode.fraOgMed.plusDays(4),
                meldekortId = korrigertMeldekortId,
                tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                antallDager = 3,
            ),
        ).plus(
            ObjectMother.tiltaksdager(
                startDato = førstePeriode.fraOgMed.plusDays(7),
                meldekortId = korrigertMeldekortId,
                tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                antallDager = 4,
            ),
        ).plus(
            ObjectMother.ikkeTiltaksdager(
                startDato = førstePeriode.fraOgMed.plusDays(11),
                meldekortId = korrigertMeldekortId,
                tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
                antallDager = 3,
            ),
        ).toNonEmptyListOrNull()!!

        val førsteUtbetalingsvedtak = lagUtbetalingsVedtak(
            id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S6"),
            meldekortId = førsteMeldekortId,
            periode = førstePeriode,
            opprettet = LocalDateTime.parse("2025-01-18T00:00:00.000001"),
            dager = ObjectMother.maksAntallDeltattTiltaksdagerIMeldekortperiode(
                startDato = førstePeriode.fraOgMed,
                meldekortId = førsteMeldekortId,
                tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
            ),
        )

        val andreUtbetalingsvedtak = lagUtbetalingsVedtak(
            id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S7"),
            meldekortId = andreMeldekortId,
            periode = andrePeriode,
            opprettet = LocalDateTime.parse("2025-01-25T00:00:00.000001"),
            dager = andrePeriodeDager,
        )

        val korrigertUtbetalingsvedtak = lagUtbetalingsVedtak(
            id = VedtakId.fromString("vedtak_01J94XH6CKY0SZ5FBEE6YZG8S8"),
            meldekortId = korrigertMeldekortId,
            periode = førstePeriode,
            opprettet = LocalDateTime.parse("2025-01-26T00:00:00.000001"),
            dager = korrigerteDager,
        )

        val førsteJson = førsteUtbetalingsvedtak.toDTO(null)
        val andreJson = andreUtbetalingsvedtak.toDTO(førsteJson)

        @Language("JSON")
        val forventetKorrigertJson =
            """
            {
              "sakId": "202410011001",
              "behandlingId": "0SZ5FBEE6YZG8S8",
              "iverksettingId": null,
              "personident": {
                "verdi": "09863149336"
              },
              "vedtak": {
                "vedtakstidspunkt": "2025-01-26T00:00:00.000001",
                "saksbehandlerId": "saksbehandler",
                "beslutterId": "beslutter",
                "utbetalinger": [
                  {
                    "beløp": 298,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2025-01-20",
                    "tilOgMedDato": "2025-01-24",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2025-01-20/2025-02-02"
                    }
                  },
                  {
                    "beløp": 298,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2025-01-27",
                    "tilOgMedDato": "2025-01-31",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2025-01-20/2025-02-02"
                    }
                  },
                  {
                    "beløp": 298,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2025-01-06",
                    "tilOgMedDato": "2025-01-09",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2025-01-06/2025-01-19"
                    }
                  },
                  {
                    "beløp": 298,
                    "satstype": "DAGLIG_INKL_HELG",
                    "fraOgMedDato": "2025-01-13",
                    "tilOgMedDato": "2025-01-16",
                    "stønadsdata": {
                      "stønadstype": "GRUPPE_AMO",
                      "barnetillegg": false,
                      "brukersNavKontor": "0220",
                      "meldekortId": "2025-01-06/2025-01-19"
                    }
                  }
                ]
              },
              "forrigeIverksetting": null
            }
            """.trimIndent()

        val korrigertJson = korrigertUtbetalingsvedtak.toDTO(andreJson)

        korrigertJson.shouldEqualJson(forventetKorrigertJson)
    }
}
