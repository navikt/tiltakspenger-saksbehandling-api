package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import org.junit.jupiter.api.Test

/**
 * Pinner json-kontrakten frontend leser grunnen til en automatisk opprettet revurdering fra.
 */
class AutomatiskOpprettetRevurderingGrunnDTOTest {

    private fun TiltaksdeltakerEndring.tilJson(): String = serialize(AutomatiskOpprettetRevurderingGrunn(this).toDTO())

    @Test
    fun `endringer uten detaljer blir én endring med bare type`() {
        TiltaksdeltakerEndring.AvsluttetSomForventet.tilJson() shouldEqualJson """{"endringer":[{"type":"AVSLUTTET_SOM_FORVENTET"}]}"""
        TiltaksdeltakerEndring.AvbruttDeltakelse.tilJson() shouldEqualJson """{"endringer":[{"type":"AVBRUTT_DELTAKELSE"}]}"""
        TiltaksdeltakerEndring.IkkeAktuellDeltakelse.tilJson() shouldEqualJson """{"endringer":[{"type":"IKKE_AKTUELL_DELTAKELSE"}]}"""
    }

    @Test
    fun `forlengelse uten endret mengde`() {
        TiltaksdeltakerEndring.Forlengelse(nySluttdato = 31.mai(2025)).tilJson() shouldEqualJson
            """{"endringer":[{"type":"FORLENGELSE","nySluttdato":"2025-05-31"}]}"""
    }

    @Test
    fun `forlengelse med endret mengde gir mengdeendringen før forlengelsen`() {
        TiltaksdeltakerEndring.Forlengelse(
            nySluttdato = 31.mai(2025),
            endretDeltakelsesmengde = TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = 50F, nyDagerPerUke = 2.5F),
        ).tilJson() shouldEqualJson
            """
            {
              "endringer": [
                {"type":"ENDRET_DELTAKELSESMENGDE","nyDeltakelsesprosent":50.0,"nyDagerPerUke":2.5},
                {"type":"FORLENGELSE","nySluttdato":"2025-05-31"}
              ]
            }
            """.trimIndent()
    }

    @Test
    fun `andre endringer gir alle detaljene i fast rekkefølge`() {
        TiltaksdeltakerEndring.AndreEndringer(
            endretStatus = TiltaksdeltakerEndring.EndretStatus(TiltakDeltakerstatus.HarSluttet),
            endretSluttdato = TiltaksdeltakerEndring.EndretSluttdato(30.april(2025)),
            endretStartdato = TiltaksdeltakerEndring.EndretStartdato(1.april(2025)),
            endretDeltakelsesmengde = TiltaksdeltakerEndring.EndretDeltakelsesmengde(nyDeltakelsesprosent = 80F, nyDagerPerUke = null),
        ).tilJson() shouldEqualJson
            """
            {
              "endringer": [
                {"type":"ENDRET_DELTAKELSESMENGDE","nyDeltakelsesprosent":80.0,"nyDagerPerUke":null},
                {"type":"ENDRET_STARTDATO","nyStartdato":"2025-04-01"},
                {"type":"ENDRET_SLUTTDATO","nySluttdato":"2025-04-30"},
                {"type":"ENDRET_STATUS","nyStatus":"HarSluttet"}
              ]
            }
            """.trimIndent()
    }

    @Test
    fun `fjernet start- og sluttdato sendes som null`() {
        TiltaksdeltakerEndring.AndreEndringer(
            endretStartdato = TiltaksdeltakerEndring.EndretStartdato(null),
            endretSluttdato = TiltaksdeltakerEndring.EndretSluttdato(null),
        ).tilJson() shouldEqualJson
            """
            {
              "endringer": [
                {"type":"ENDRET_STARTDATO","nyStartdato":null},
                {"type":"ENDRET_SLUTTDATO","nySluttdato":null}
              ]
            }
            """.trimIndent()
    }
}
