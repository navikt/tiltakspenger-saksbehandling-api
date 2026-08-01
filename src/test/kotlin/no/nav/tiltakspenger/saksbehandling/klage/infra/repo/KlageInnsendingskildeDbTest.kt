package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageInnsendingskilde
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlageInnsendingskildeDb.Companion.toDb
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Innsendingskilden ligger i formkrav-jsonb-en på klagebehandlingen, med én gren per kilde i hver retning.
 * Én klage har én kilde, så det ville tatt fem klageflyter å nå alle grenene gjennom prodstien — for en mapping som ikke rører postgres.
 *
 * Testen pinner navnene som havner i jsonb-en, ikke bare rundturen.
 */
class KlageInnsendingskildeDbTest {

    @Test
    fun `innsendingskildene lagres med sitt avtalte navn`() {
        KlageInnsendingskilde.entries.associateWith { it.toDb().name } shouldBe mapOf(
            KlageInnsendingskilde.DIGITAL to "DIGITAL",
            KlageInnsendingskilde.PAPIR_SKJEMA to "PAPIR_SKJEMA",
            KlageInnsendingskilde.PAPIR_FRIHAND to "PAPIR_FRIHAND",
            KlageInnsendingskilde.MODIA to "MODIA",
            KlageInnsendingskilde.ANNET to "ANNET",
        )
    }

    @Test
    fun `innsendingskildene leses tilbake fra lagret navn`() {
        KlageInnsendingskildeDb.entries.forEach {
            it.toDomain().toDb() shouldBe it
        }
    }
}
