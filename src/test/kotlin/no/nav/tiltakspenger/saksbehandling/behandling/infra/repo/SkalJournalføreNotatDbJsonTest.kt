package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import org.junit.jupiter.api.Test

class SkalJournalføreNotatDbJsonTest {

    private val skalJournalføreNotat = SkalJournalføreNotat(vilkårsvurdering = true, barnetillegg = false)

    private val lagretJson = """{"vilkårsvurdering":true,"barnetillegg":false}"""

    @Test
    fun `toDbJson() matcher med det som skal bli lagret`() {
        skalJournalføreNotat.toDbJson().shouldEqualJson(lagretJson)
    }

    @Test
    fun `lagret json er den samme som domain `() {
        lagretJson.toSkalJournalføreNotat() shouldBe skalJournalføreNotat
    }

    @Test
    fun `default leses som ingen`() {
        """{"vilkårsvurdering": false, "barnetillegg": false}""".toSkalJournalføreNotat() shouldBe SkalJournalføreNotat.INGEN
    }
}
