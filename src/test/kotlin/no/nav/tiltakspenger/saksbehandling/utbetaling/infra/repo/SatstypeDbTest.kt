package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * [Satstype] er utsjekk-kontraktens enum, ikke vår egen, og de to satstypene vi ikke bruker kan ikke nås fra en prodsti i det hele tatt.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 */
class SatstypeDbTest {

    @Test
    fun `satstypene vi bruker lagres med sitt avtalte navn`() {
        Satstype.DAGLIG.tilDb() shouldBe "DAGLIG"
        Satstype.DAGLIG_INKL_HELG.tilDb() shouldBe "DAGLIG_INKL_HELG"
    }

    @Test
    fun `satstypene vi bruker leses tilbake fra lagret verdi`() {
        "DAGLIG".tilSatstype() shouldBe Satstype.DAGLIG
        "DAGLIG_INKL_HELG".tilSatstype() shouldBe Satstype.DAGLIG_INKL_HELG
    }

    /**
     * Kontrakten har to satstyper til.
     * Tiltakspenger utbetales per dag, så havner en av dem i en utbetaling er det en feil vi vil vite om før den lagres.
     */
    @Test
    fun `satstypene vi ikke bruker kan ikke lagres`() {
        shouldThrowWithMessage<IllegalArgumentException>("Vi bruker ikke satstypen MÅNEDLIG") {
            Satstype.MÅNEDLIG.tilDb()
        }
        shouldThrowWithMessage<IllegalArgumentException>("Vi bruker ikke satstypen ENGANGS") {
            Satstype.ENGANGS.tilDb()
        }
    }
}
