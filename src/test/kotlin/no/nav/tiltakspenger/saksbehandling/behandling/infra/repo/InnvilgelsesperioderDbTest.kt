package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import org.junit.jupiter.api.Test

/**
 * Pinner hvordan innvilgelsesperiodene lagres i jsonb-kolonnen.
 * Hver periode har en liste med tiltaksdeltakelser, hver med sitt eget antall dager per meldeperiode.
 * Markeringen av reversert opphør utelates helt for vanlige innvilgelser.
 */
class InnvilgelsesperioderDbTest {

    private val opprinneligVedtakId = VedtakId.fromString("vedtak_01JQ0000000000000000000000")

    private val amo = ObjectMother.tiltaksdeltakelse(
        periode = 1.januar(2025) til 31.mars(2025),
        eksternTiltaksdeltakelseId = "ekstern-amo",
        internDeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KEF73CZJX0MKYG4NK27BV7HG"),
    )
    private val arbeidstrening = ObjectMother.tiltaksdeltakelse(
        periode = 1.januar(2025) til 28.februar(2025),
        eksternTiltaksdeltakelseId = "ekstern-arbeidstrening",
        tiltakstype = TiltakstypeSomGirRett.ARBEIDSTRENING,
        typeNavn = "Arbeidstrening",
        internDeltakelseId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01KEXQCG2FZV0629GX7QM4W1DV"),
    )

    private val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
        ObjectMother.innvilgelsesperiode(
            periode = 1.januar(2025) til 28.februar(2025),
            tiltaksdeltakelser = nonEmptyListOf(
                ObjectMother.innvilgetTiltaksdeltakelse(amo, AntallDagerForMeldeperiode(4)),
                ObjectMother.innvilgetTiltaksdeltakelse(arbeidstrening, AntallDagerForMeldeperiode(6)),
            ),
        ),
        ObjectMother.innvilgelsesperiode(
            periode = 1.mars(2025) til 31.mars(2025),
            tiltaksdeltakelser = nonEmptyListOf(ObjectMother.innvilgetTiltaksdeltakelse(amo, AntallDagerForMeldeperiode(10))),
            reversertOpphør = ReversertOpphør(opprinneligVedtakId),
        ),
    )

    @Test
    fun `lagres med en liste av tiltaksdeltakelser per periode`() {
        innvilgelsesperioder.tilInnvilgelsesperioderDbJson() shouldEqualJson """
            {
              "value": [
                {
                  "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-02-28"},
                  "tiltaksdeltakelser": [
                    {"tiltaksdeltakelse": ${tiltaksdeltakelseJson(amo.internDeltakelseId, "ekstern-amo", "GRUPPE_AMO", "Arbeidsmarkedsoppfølging gruppe", "2025-03-31")}, "antallDagerPerMeldeperiode": 4},
                    {"tiltaksdeltakelse": ${tiltaksdeltakelseJson(arbeidstrening.internDeltakelseId, "ekstern-arbeidstrening", "ARBEIDSTRENING", "Arbeidstrening", "2025-02-28")}, "antallDagerPerMeldeperiode": 6}
                  ]
                },
                {
                  "periode": {"fraOgMed": "2025-03-01", "tilOgMed": "2025-03-31"},
                  "tiltaksdeltakelser": [
                    {"tiltaksdeltakelse": ${tiltaksdeltakelseJson(amo.internDeltakelseId, "ekstern-amo", "GRUPPE_AMO", "Arbeidsmarkedsoppfølging gruppe", "2025-03-31")}, "antallDagerPerMeldeperiode": 10}
                  ],
                  "reversertOpphør": {"opprinneligVedtakId": "vedtak_01JQ0000000000000000000000"}
                }
              ]
            }
        """.trimIndent()
    }

    @Test
    fun `leses tilbake fra lagret json`() {
        innvilgelsesperioder.tilInnvilgelsesperioderDbJson().tilInnvilgelsesperioder() shouldBe innvilgelsesperioder
    }

    private fun tiltaksdeltakelseJson(
        internDeltakelseId: TiltaksdeltakerId,
        eksternDeltakelseId: String,
        tiltakstype: String,
        typeNavn: String,
        tilOgMed: String,
    ): String = """
        {
          "eksternDeltagelseId": "$eksternDeltakelseId",
          "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
          "typeNavn": "$typeNavn",
          "tittel": null,
          "typeKode": "$tiltakstype",
          "deltagelseFraOgMed": "2025-01-01",
          "deltagelseTilOgMed": "$tilOgMed",
          "deltakelseStatus": "Deltar",
          "deltakelseProsent": 100.0,
          "antallDagerPerUke": 5.0,
          "kilde": "Komet",
          "rettPåTiltakspenger": true,
          "deltidsprosentGjennomforing": null,
          "internDeltakelseId": "$internDeltakelseId"
        }
    """.trimIndent()
}
