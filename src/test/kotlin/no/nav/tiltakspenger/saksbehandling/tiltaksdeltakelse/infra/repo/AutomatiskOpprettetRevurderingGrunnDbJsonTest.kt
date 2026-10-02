package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Hver endringstype ville krevd sin egen deltakerhendelse fra Komet eller Arena for å nås fra en prodsti, og mappingen rører ikke postgres.
 *
 * Testen pinner **den faktiske json-en** som havner i kolonnen, ikke bare rundturen.
 * Db-typene er `private`, så json-strengen er den eneste kontrakten som er synlig utenfra — og den er kontrakten mot rader som allerede er lagret.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomatiskOpprettetRevurderingGrunnDbJsonTest {

    private fun grunn(endring: TiltaksdeltakerEndring) = AutomatiskOpprettetRevurderingGrunn(endring = endring)

    private fun json(vararg endringer: String): String =
        """{ "endringer": [${endringer.joinToString(",")}] }"""

    private fun endringJson(
        type: String,
        nySluttdato: String? = null,
        nyStartdato: String? = null,
        nyDeltakelsesprosent: Float? = null,
        nyDagerPerUke: Float? = null,
        nyStatus: String? = null,
    ): String {
        fun String?.q() = this?.let { "\"$it\"" } ?: "null"
        return """{ "type": "$type", "nySluttdato": ${nySluttdato.q()}, "nyStartdato": ${nyStartdato.q()}, "nyDeltakelsesprosent": $nyDeltakelsesprosent, "nyDagerPerUke": $nyDagerPerUke, "nyStatus": ${nyStatus.q()} }"""
    }

    private val mengde = TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = 60F, nyDagerPerUke = 3F)
    private val mengdeJson = endringJson("ENDRET_DELTAKELSESMENGDE", nyDeltakelsesprosent = 60F, nyDagerPerUke = 3F)

    fun endringerMedLagretJson(): List<Arguments> = listOf(
        Arguments.of(TiltaksdeltakerEndring.AvsluttetSomForventet, json(endringJson("AVSLUTTET_SOM_FORVENTET"))),
        Arguments.of(TiltaksdeltakerEndring.AvbruttDeltakelse, json(endringJson("AVBRUTT_DELTAKELSE"))),
        Arguments.of(TiltaksdeltakerEndring.IkkeAktuellDeltakelse, json(endringJson("IKKE_AKTUELL_DELTAKELSE"))),
        Arguments.of(
            TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mars(2025)),
            json(endringJson("FORLENGELSE", nySluttdato = "2025-03-31")),
        ),
        Arguments.of(
            TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mars(2025), endretDeltakelsesmengde = mengde),
            json(mengdeJson, endringJson("FORLENGELSE", nySluttdato = "2025-03-31")),
        ),
        Arguments.of(
            TiltaksdeltakerEndring.AndreEndringer(
                endretDeltakelsesmengde = mengde,
                endretStartdato = TiltaksdeltakerEndring.EndretStartdato(nyStartdato = 1.mars(2025)),
                endretSluttdato = TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = 15.mars(2025)),
                endretStatus = TiltaksdeltakerEndring.EndretStatus(nyStatus = TiltakDeltakerstatus.HarSluttet),
            ),
            json(
                mengdeJson,
                endringJson("ENDRET_STARTDATO", nyStartdato = "2025-03-01"),
                endringJson("ENDRET_SLUTTDATO", nySluttdato = "2025-03-15"),
                endringJson("ENDRET_STATUS", nyStatus = "HarSluttet"),
            ),
        ),
        Arguments.of(
            TiltaksdeltakerEndring.AndreEndringer(
                endretStartdato = TiltaksdeltakerEndring.EndretStartdato(nyStartdato = null),
                endretSluttdato = TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = null),
            ),
            json(endringJson("ENDRET_STARTDATO"), endringJson("ENDRET_SLUTTDATO")),
        ),
    )

    @ParameterizedTest
    @MethodSource("endringerMedLagretJson")
    fun `endringen lagres med sine avtalte navn og felter`(endring: TiltaksdeltakerEndring, forventetJson: String) {
        grunn(endring).toDbJson() shouldEqualJson forventetJson
    }

    @ParameterizedTest
    @MethodSource("endringerMedLagretJson")
    fun `endringen leses tilbake fra lagret json`(endring: TiltaksdeltakerEndring, lagretJson: String) {
        lagretJson.toAutomatiskOpprettetRevurderingGrunn() shouldBe grunn(endring)
    }

    @Test
    fun `eldre rad med hendelseId leses`() {
        """{ "hendelseId": "01JQ8Z4XW9K5N2P7R3T6V8Y1BC", "endringer": [${endringJson("AVBRUTT_DELTAKELSE")}] }""".toAutomatiskOpprettetRevurderingGrunn() shouldBe
            AutomatiskOpprettetRevurderingGrunn(endring = TiltaksdeltakerEndring.AvbruttDeltakelse)
    }

    @Test
    fun `eldre rad med flere utfall leses som utfallet med høyest prioritet`() {
        json(
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
            endringJson("IKKE_AKTUELL_DELTAKELSE"),
            endringJson("AVBRUTT_DELTAKELSE"),
            endringJson("AVSLUTTET_SOM_FORVENTET"),
        ).toAutomatiskOpprettetRevurderingGrunn().endring shouldBe TiltaksdeltakerEndring.AvsluttetSomForventet

        json(
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
            endringJson("IKKE_AKTUELL_DELTAKELSE"),
            endringJson("AVBRUTT_DELTAKELSE"),
        ).toAutomatiskOpprettetRevurderingGrunn().endring shouldBe TiltaksdeltakerEndring.AvbruttDeltakelse

        json(
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
            endringJson("IKKE_AKTUELL_DELTAKELSE"),
        ).toAutomatiskOpprettetRevurderingGrunn().endring shouldBe TiltaksdeltakerEndring.IkkeAktuellDeltakelse

        json(
            endringJson("ENDRET_STARTDATO", nyStartdato = "2025-03-01"),
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
        ).toAutomatiskOpprettetRevurderingGrunn().endring shouldBe
            TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mars(2025), endretDeltakelsesmengde = mengde)
    }
}
