package no.nav.tiltakspenger.saksbehandling.behandling.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import org.junit.jupiter.api.Test

class BehandlingIdExTest {

    @Test
    fun `match dispatcher RammebehandlingId til rammebehandlingId-branchen`() {
        val id: BehandlingId = RammebehandlingId.random()

        val result = id.match(
            rammebehandlingId = { "ramme:$it" },
            meldekortId = { "meldekort:$it" },
        )

        result shouldBe "ramme:$id"
    }

    @Test
    fun `match dispatcher MeldekortId til meldekortId-branchen`() {
        val id: BehandlingId = MeldekortId.random()

        val result = id.match(
            rammebehandlingId = { "ramme:$it" },
            meldekortId = { "meldekort:$it" },
        )

        result shouldBe "meldekort:$id"
    }

    @Test
    fun `match gir typed verdi tilbake`() {
        val rammeId = RammebehandlingId.random()
        val meldekortId = MeldekortId.random()

        val rammeResult: RammebehandlingId = (rammeId as BehandlingId).match(
            rammebehandlingId = { it },
            meldekortId = { error("skal ikke kalles") },
        )
        val meldekortResult: MeldekortId = (meldekortId as BehandlingId).match(
            rammebehandlingId = { error("skal ikke kalles") },
            meldekortId = { it },
        )

        rammeResult shouldBe rammeId
        meldekortResult shouldBe meldekortId
    }

    @Test
    fun `match kaller kun den matchende branchen`() {
        var rammeKalt = 0
        var meldekortKalt = 0
        val id: BehandlingId = RammebehandlingId.random()

        id.match(
            rammebehandlingId = { rammeKalt++ },
            meldekortId = { meldekortKalt++ },
        )

        rammeKalt shouldBe 1
        meldekortKalt shouldBe 0
    }

    @Test
    fun `match kaster exception for ukjent behandlingId`() {
        val ukjent: BehandlingId = KlagebehandlingId.random()

        val ex = shouldThrow<IllegalStateException> {
            ukjent.match(
                rammebehandlingId = { error("skal ikke kalles") },
                meldekortId = { error("skal ikke kalles") },
            )
        }
        ex.message shouldBe "Ukjent BehandlingId-type: ${ukjent::class}"
    }
}
