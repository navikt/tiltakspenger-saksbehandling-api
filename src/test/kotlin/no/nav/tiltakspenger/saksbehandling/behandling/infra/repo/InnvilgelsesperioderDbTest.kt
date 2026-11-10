package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Pinner hvordan markeringen av reversert opphør lagres i jsonb-kolonnen for innvilgelsesperiodene.
 * Feltet utelates helt for vanlige innvilgelser, slik at allerede lagrede rader er uendret.
 */
class InnvilgelsesperioderDbTest {

    private val opprinneligVedtakId = VedtakId.fromString("vedtak_01JQ0000000000000000000000")

    private val innvilgelsesperioder = ObjectMother.innvilgelsesperioder(
        ObjectMother.innvilgelsesperiode(periode = 1.januar(2025) til 28.februar(2025)),
        ObjectMother.innvilgelsesperiode(
            periode = 1.mars(2025) til 31.mars(2025),
            reversertOpphør = ReversertOpphør(opprinneligVedtakId),
        ),
    )

    @Test
    fun `reversert opphør lagres med opprinnelig vedtak og utelates for vanlige innvilgelser`() {
        val lagret = JSONObject(innvilgelsesperioder.tilInnvilgelsesperioderDbJson()).getJSONArray("value")

        lagret.getJSONObject(0).has("reversertOpphør") shouldBe false
        lagret.getJSONObject(1).getJSONObject("reversertOpphør").toString() shouldBe
            """{"opprinneligVedtakId":"vedtak_01JQ0000000000000000000000"}"""
    }

    @Test
    fun `reversert opphør leses tilbake fra lagret json`() {
        innvilgelsesperioder.tilInnvilgelsesperioderDbJson().tilInnvilgelsesperioder() shouldBe innvilgelsesperioder
    }
}
