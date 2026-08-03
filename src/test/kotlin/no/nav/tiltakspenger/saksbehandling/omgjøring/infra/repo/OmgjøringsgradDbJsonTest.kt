package no.nav.tiltakspenger.saksbehandling.omgjøring.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * En prodsti per grad ville kostet en egen omgjøringsflyt hver, mens mappingen ikke rører postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 */
class OmgjøringsgradDbJsonTest {

    @Test
    fun `gradene lagres med sitt avtalte navn`() {
        Omgjøringsgrad.entries.associateWith { it.toDbJson() } shouldBe mapOf(
            Omgjøringsgrad.HELT to OmgjøringsgradDbJson.HELT,
            Omgjøringsgrad.DELVIS to OmgjøringsgradDbJson.DELVIS,
        )
    }

    @Test
    fun `gradene leses tilbake fra lagret verdi`() {
        OmgjøringsgradDbJson.entries.forEach {
            it.toDomain() shouldBe Omgjøringsgrad.valueOf(it.name)
        }
    }
}
