package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndringer
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Hver endringstype ville krevd sin egen deltakerhendelse fra Komet eller Arena for å nås fra en prodsti, og mappingen rører ikke postgres.
 *
 * Testen pinner **den faktiske json-en** som havner i kolonnen, ikke bare rundturen.
 * Db-typene er `private`, så json-strengen er den eneste kontrakten som er synlig utenfra — og den er kontrakten mot rader som allerede er lagret.
 */
class AutomatiskOpprettetRevurderingGrunnDbJsonTest {

    private val grunn = AutomatiskOpprettetRevurderingGrunn(
        hendelseId = "01JQ8Z4XW9K5N2P7R3T6V8Y1BC",
        endringer = TiltaksdeltakerEndringer(
            nonEmptyListOf(
                TiltaksdeltakerEndring.AvbruttDeltakelse,
                TiltaksdeltakerEndring.IkkeAktuellDeltakelse,
                TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mars(2025)),
                TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = 15.mars(2025)),
                TiltaksdeltakerEndring.EndretSluttdato(nySluttdato = null),
                TiltaksdeltakerEndring.EndretStartdato(nyStartdato = 1.mars(2025)),
                TiltaksdeltakerEndring.EndretStartdato(nyStartdato = null),
                TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = 60F, nyDagerPerUke = 3F),
                TiltaksdeltakerEndring.EndretStatus(nyStatus = TiltakDeltakerstatus.HarSluttet),
            ),
        ),
    )

    @Test
    fun `endringene lagres med sine avtalte navn og felter`() {
        //language=json
        grunn.toDbJson() shouldEqualJson """
            {
              "hendelseId": "01JQ8Z4XW9K5N2P7R3T6V8Y1BC",
              "endringer": [
                { "type": "AVBRUTT_DELTAKELSE", "nySluttdato": null, "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "IKKE_AKTUELL_DELTAKELSE", "nySluttdato": null, "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "FORLENGELSE", "nySluttdato": "2025-03-31", "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "ENDRET_SLUTTDATO", "nySluttdato": "2025-03-15", "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "ENDRET_SLUTTDATO", "nySluttdato": null, "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "ENDRET_STARTDATO", "nySluttdato": null, "nyStartdato": "2025-03-01", "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "ENDRET_STARTDATO", "nySluttdato": null, "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": null },
                { "type": "ENDRET_DELTAKELSESMENGDE", "nySluttdato": null, "nyStartdato": null, "nyDeltakelsesprosent": 60.0, "nyDagerPerUke": 3.0, "nyStatus": null },
                { "type": "ENDRET_STATUS", "nySluttdato": null, "nyStartdato": null, "nyDeltakelsesprosent": null, "nyDagerPerUke": null, "nyStatus": "HarSluttet" }
              ]
            }
        """.trimIndent()
    }

    @Test
    fun `endringene leses tilbake fra lagret json`() {
        grunn.toDbJson().toAutomatiskOpprettetRevurderingGrunn() shouldBe grunn
    }
}
