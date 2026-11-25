package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettedeRevurderingGrunner
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Hver endringstype ville krevd sin egen deltakerhendelse fra Komet eller Arena for å nås fra en prodsti, og mappingen rører ikke postgres.
 *
 * Testen pinner **den faktiske json-en** som havner i kolonnen, ikke bare rundturen.
 * Db-typene er `private`, så json-strengen er den eneste kontrakten som er synlig utenfra — og den er kontrakten mot rader som allerede er lagret.
 */
class AutomatiskOpprettetRevurderingGrunnDbJsonTest {

    private val tiltaksdeltakerId = TiltaksdeltakerId.fromString("tiltaksdeltaker_01JQ8Z4XW9K5N2P7R3T6V8Y1BC")

    private val opprettet = LocalDateTime.of(2025, 3, 1, 9, 30, 15, 123_456_000)
    private val lagtTil = LocalDateTime.of(2025, 3, 2, 12, 0)
    private val lagtTilJson = "2025-03-02T12:00"

    private fun grunn(endring: TiltaksdeltakerEndring, lagtTil: LocalDateTime = this.lagtTil) =
        AutomatiskOpprettetRevurderingGrunn(endring = endring, tiltaksdeltakerId = tiltaksdeltakerId, lagtTil = lagtTil)

    private fun grunner(endring: TiltaksdeltakerEndring) = AutomatiskOpprettedeRevurderingGrunner(grunn(endring))

    /** Én grunn på det eldre formatet, med feltene direkte på rotnivå og uten `lagtTil`. */
    private fun json(vararg endringer: String): String =
        """{ "endringer": [${endringer.joinToString(",")}], "tiltaksdeltakerId": "$tiltaksdeltakerId" }"""

    private fun grunnJson(vararg endringer: String, lagtTil: String = lagtTilJson): String =
        """{ "endringer": [${endringer.joinToString(",")}], "tiltaksdeltakerId": "$tiltaksdeltakerId", "lagtTil": "$lagtTil" }"""

    private fun lagret(vararg grunner: String): String = """{ "grunner": [${grunner.joinToString(",")}] }"""

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

    fun endringerMedLagretJsonPåEldreFormat(): List<Arguments> = endringerMedLagretJson(eldreFormat = true)

    fun endringerMedLagretJson(): List<Arguments> = endringerMedLagretJson(eldreFormat = false)

    private fun endringerMedLagretJson(eldreFormat: Boolean): List<Arguments> {
        fun json(vararg endringer: String): String = if (eldreFormat) this.json(*endringer) else grunnJson(*endringer)
        return listOf(
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
                TiltaksdeltakerEndring.Avkortet(nySluttdato = 15.mars(2025)),
                json(endringJson("AVKORTET", nySluttdato = "2025-03-15")),
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
    }

    @ParameterizedTest
    @MethodSource("endringerMedLagretJson")
    fun `endringen lagres med sine avtalte navn og felter`(endring: TiltaksdeltakerEndring, forventetGrunnJson: String) {
        grunner(endring).toDbJson() shouldEqualJson lagret(forventetGrunnJson)
    }

    @ParameterizedTest
    @MethodSource("endringerMedLagretJson")
    fun `endringen leses tilbake fra lagret json`(endring: TiltaksdeltakerEndring, lagretGrunnJson: String) {
        lagret(lagretGrunnJson).toAutomatiskOpprettedeRevurderingGrunner(opprettet) shouldBe grunner(endring)
    }

    @ParameterizedTest
    @MethodSource("endringerMedLagretJsonPåEldreFormat")
    fun `eldre rad med én grunn leses med opprettet som tidspunktet grunnen ble lagt til`(endring: TiltaksdeltakerEndring, lagretJson: String) {
        lagretJson.toAutomatiskOpprettedeRevurderingGrunner(opprettet) shouldBe
            AutomatiskOpprettedeRevurderingGrunner(grunn(endring, lagtTil = opprettet))
    }

    @Test
    fun `flere grunner lagres og leses i rekkefølgen de ble lagt til`() {
        val grunner = AutomatiskOpprettedeRevurderingGrunner(grunn(TiltaksdeltakerEndring.IkkeAktuellDeltakelse, lagtTil = opprettet))
            .leggTil(grunn(TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mars(2025))))
        val forventet = lagret(
            grunnJson(endringJson("IKKE_AKTUELL_DELTAKELSE"), lagtTil = "2025-03-01T09:30:15.123456"),
            grunnJson(endringJson("FORLENGELSE", nySluttdato = "2025-03-31")),
        )

        grunner.toDbJson() shouldEqualJson forventet
        forventet.toAutomatiskOpprettedeRevurderingGrunner(opprettet) shouldBe grunner
    }

    @Test
    fun `eldre rad med hendelseId og uten tiltaksdeltakerId leses`() {
        """{ "hendelseId": "01JQ8Z4XW9K5N2P7R3T6V8Y1BC", "endringer": [${endringJson("AVBRUTT_DELTAKELSE")}] }""".toAutomatiskOpprettedeRevurderingGrunner(opprettet) shouldBe
            AutomatiskOpprettedeRevurderingGrunner(
                AutomatiskOpprettetRevurderingGrunn(endring = TiltaksdeltakerEndring.AvbruttDeltakelse, tiltaksdeltakerId = null, lagtTil = opprettet),
            )
    }

    @Test
    fun `migrert rad uten hendelseId og med lagtTil fra databasen leses`() {
        lagret("""{ "endringer": [${endringJson("AVBRUTT_DELTAKELSE")}], "lagtTil": "2025-03-01T09:30:15.123456" }""")
            .toAutomatiskOpprettedeRevurderingGrunner(LocalDateTime.of(2000, 1, 1, 0, 0)) shouldBe
            AutomatiskOpprettedeRevurderingGrunner(
                AutomatiskOpprettetRevurderingGrunn(endring = TiltaksdeltakerEndring.AvbruttDeltakelse, tiltaksdeltakerId = null, lagtTil = opprettet),
            )
    }

    @Test
    fun `grunn uten tiltaksdeltakerId lagres uten id`() {
        AutomatiskOpprettedeRevurderingGrunner(
            AutomatiskOpprettetRevurderingGrunn(endring = TiltaksdeltakerEndring.AvbruttDeltakelse, tiltaksdeltakerId = null, lagtTil = lagtTil),
        ).toDbJson() shouldEqualJson
            lagret("""{ "endringer": [${endringJson("AVBRUTT_DELTAKELSE")}], "tiltaksdeltakerId": null, "lagtTil": "$lagtTilJson" }""")
    }

    @Test
    fun `eldre rad med flere utfall leses som utfallet med høyest prioritet`() {
        json(
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
            endringJson("IKKE_AKTUELL_DELTAKELSE"),
            endringJson("AVBRUTT_DELTAKELSE"),
            endringJson("AVSLUTTET_SOM_FORVENTET"),
        ).toAutomatiskOpprettedeRevurderingGrunner(opprettet).første.endring shouldBe TiltaksdeltakerEndring.AvsluttetSomForventet

        json(
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
            endringJson("IKKE_AKTUELL_DELTAKELSE"),
            endringJson("AVBRUTT_DELTAKELSE"),
        ).toAutomatiskOpprettedeRevurderingGrunner(opprettet).første.endring shouldBe TiltaksdeltakerEndring.AvbruttDeltakelse

        json(
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
            endringJson("IKKE_AKTUELL_DELTAKELSE"),
        ).toAutomatiskOpprettedeRevurderingGrunner(opprettet).første.endring shouldBe TiltaksdeltakerEndring.IkkeAktuellDeltakelse

        json(
            endringJson("ENDRET_STARTDATO", nyStartdato = "2025-03-01"),
            mengdeJson,
            endringJson("FORLENGELSE", nySluttdato = "2025-03-31"),
        ).toAutomatiskOpprettedeRevurderingGrunner(opprettet).første.endring shouldBe
            TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mars(2025), endretDeltakelsesmengde = mengde)
    }
}
