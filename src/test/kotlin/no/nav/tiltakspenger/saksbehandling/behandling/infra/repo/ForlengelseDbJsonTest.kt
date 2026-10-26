package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ForlengerRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import org.junit.jupiter.api.Test

class ForlengelseDbJsonTest {

    private val forlengelse = Revurderingsresultat.Forlengelse.opprett(
        ForlengerRammevedtak(
            rammevedtakId = VedtakId.fromString("vedtak_01KAJ7K0Y7SJ1Y5BN5MN8Z9FDV"),
            forlengesFraOgMed = 1.april(2025),
        ),
    )

    private val lagretJson = """{"rammevedtakId":"vedtak_01KAJ7K0Y7SJ1Y5BN5MN8Z9FDV","forlengesFraOgMed":"2025-04-01","harEndretBarnetillegg":false}"""

    @Test
    fun `tilForlengelseDbJson() matcher med det som skal bli lagret`() {
        forlengelse.tilForlengelseDbJson().shouldEqualJson(lagretJson)
    }

    @Test
    fun `lagret json er den samme som domenet`() {
        lagretJson.tilForlengelse(innvilgelsesperioder = null, barnetillegg = null) shouldBe forlengelse
    }
}
