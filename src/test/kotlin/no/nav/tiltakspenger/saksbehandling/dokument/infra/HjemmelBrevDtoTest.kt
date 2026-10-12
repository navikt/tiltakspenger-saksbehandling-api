package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde
import org.junit.jupiter.api.Test

class HjemmelBrevDtoTest {

    @Test
    fun `mapper hver rettskilde til koden pdfgenrs kjenner`() {
        Rettskilde.entries.associateWith { it.tilRettskildeBrevDto().name } shouldBe mapOf(
            Rettskilde.Arbeidsmarkedsloven to "ARBEIDSMARKEDSLOVEN",
            Rettskilde.Folketrygdloven to "FOLKETRYGDLOVEN",
            Rettskilde.Foreldelsesloven to "FORELDELSESLOVEN",
            Rettskilde.Forvaltningsloven to "FORVALTNINGSLOVEN",
            Rettskilde.Tiltakspengeforskriften to "TILTAKSPENGEFORSKRIFTEN",
        )
    }

    @Test
    fun `serialiserer hjemmel med og uten ledd`() {
        serialize(
            listOf(
                Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_7_L1.tilHjemmelBrevDto(),
                Hjemmel.ArbeidsmarkedslovenHjemmel.ARBEIDSMARKEDSLOVEN_13.tilHjemmelBrevDto(),
            ),
        ).shouldEqualJson(
            //language=json
            """
            [
              {"rettskilde":"TILTAKSPENGEFORSKRIFTEN","paragraf":"7","ledd":1},
              {"rettskilde":"ARBEIDSMARKEDSLOVEN","paragraf":"13","ledd":null}
            ]
            """.trimIndent(),
        )
    }
}
