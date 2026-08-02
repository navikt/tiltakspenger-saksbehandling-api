package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Posteringstype
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Posteringstypen ligger i simulerings-jsonb-en på utbetalingen, med én gren per type i hver retning.
 * Hvilke typer oppdragssystemet svarer med, styres av det de har av tidligere utbetalinger og trekk på personen — flere av dem kan ikke framprovoseres gjennom en prodsti hos oss i det hele tatt.
 *
 * Testen pinner navnene som havner i jsonb-en, ikke bare rundturen.
 * Den sier ingenting om hvorvidt hver type faktisk kan komme fra oppdragssystemet.
 */
class PosteringstypeDbTest {

    @Test
    fun `posteringstypene lagres med sitt avtalte navn`() {
        Posteringstype.entries.associateWith { it.toDbType().name } shouldBe mapOf(
            Posteringstype.YTELSE to "YTELSE",
            Posteringstype.FEILUTBETALING to "FEILUTBETALING",
            Posteringstype.FORSKUDSSKATT to "FORSKUDSSKATT",
            Posteringstype.JUSTERING to "JUSTERING",
            Posteringstype.TREKK to "TREKK",
            Posteringstype.MOTPOSTERING to "MOTPOSTERING",
        )
    }

    @Test
    fun `posteringstypene leses tilbake fra lagret navn`() {
        SimuleringEndringDbJson.PosteringstypeDbType.entries.forEach {
            it.toDomain().toDbType() shouldBe it
        }
    }
}
