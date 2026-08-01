package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Kildene eies av Arena, Komet og Team Tiltak, og en prodsti per kilde ville kostet en egen deltakelse hver.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 */
class TiltakskildeDbTest {

    @Test
    fun `kildene lagres med sitt avtalte navn`() {
        Tiltakskilde.entries.associateWith { it.toDb() } shouldBe mapOf(
            Tiltakskilde.Arena to "Arena",
            Tiltakskilde.Komet to "Komet",
            Tiltakskilde.TeamTiltak to "TeamTiltak",
        )
    }

    @Test
    fun `kildene leses tilbake fra lagret verdi`() {
        Tiltakskilde.entries.forEach {
            it.toDb().toTiltakskilde() shouldBe it
        }
    }
}
