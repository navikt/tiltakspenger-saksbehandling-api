package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Hver feilårsak ville krevd sin egen konstruerte feilsituasjon gjennom hendelsejobben, og mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * Verdiene står i `tilbakekreving_hendelse.feil` på rader som allerede er skrevet.
 */
class TilbakekrevinghendelseFeilDbTest {

    @Test
    fun `feilårsakene lagres med sitt avtalte navn`() {
        TilbakekrevinghendelseFeil.entries.associateWith { it.tilDb() } shouldBe mapOf(
            TilbakekrevinghendelseFeil.UgyldigSaksnummer to "UgyldigSaksnummer",
            TilbakekrevinghendelseFeil.FantIkkeSak to "FantIkkeSak",
            TilbakekrevinghendelseFeil.FantIkkeBehandling to "FantIkkeBehandling",
            TilbakekrevinghendelseFeil.FantIkkeUtbetaling to "FantIkkeUtbetaling",
        )
    }

    @Test
    fun `feilårsakene leses tilbake fra lagret verdi`() {
        TilbakekrevinghendelseFeil.entries.forEach {
            TilbakekrevinghendelseFeilDb.valueOf(it.tilDb()).tilDomene() shouldBe it
        }
    }
}
