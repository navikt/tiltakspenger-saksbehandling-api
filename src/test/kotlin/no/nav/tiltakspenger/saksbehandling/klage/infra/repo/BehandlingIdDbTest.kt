package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Klagens jsonb-kolonner lagrer behandlings-IDen som en ren streng, og typen utledes av prefikset når den leses tilbake.
 * Å nå hver gren gjennom prodstien ville krevd én klage per behandlingstype, og «ukjent prefiks»-grenen kan uansett bare oppstå av korrupt eller historisk data.
 *
 * Testen pinner prefiksene, som er kontrakten mot IDene som allerede ligger lagret.
 */
class BehandlingIdDbTest {

    @Test
    fun `behandlingen det klages på leses tilbake med riktig type`() {
        val rammebehandlingId = RammebehandlingId.random()
        val meldekortId = MeldekortId.random()
        val klagebehandlingId = KlagebehandlingId.random()

        rammebehandlingId.toString().toBehandlingIdDetKlagesPå() shouldBe rammebehandlingId
        meldekortId.toString().toBehandlingIdDetKlagesPå() shouldBe meldekortId
        klagebehandlingId.toString().toBehandlingIdDetKlagesPå() shouldBe klagebehandlingId
    }

    @Test
    fun `behandlingen det klages på må ha et kjent prefiks`() {
        shouldThrowWithMessage<IllegalArgumentException>(
            "Ukjent format for behandlingDetKlagesPå: sak_123. Forventet å starte med 'beh_' eller 'meldekort'.",
        ) {
            "sak_123".toBehandlingIdDetKlagesPå()
        }
    }

    @Test
    fun `behandlingen i klagebehandlingsresultatet leses tilbake med riktig type`() {
        val rammebehandlingId = RammebehandlingId.random()
        val meldekortId = MeldekortId.random()

        rammebehandlingId.toString().toBehandlingId() shouldBe rammebehandlingId
        meldekortId.toString().toBehandlingId() shouldBe meldekortId
    }

    @Test
    fun `behandlingen i klagebehandlingsresultatet må ha et kjent prefiks`() {
        shouldThrowWithMessage<IllegalArgumentException>(
            "Ukjent format for behandlingId i klagebehandlingsresultat: klage_123",
        ) {
            "klage_123".toBehandlingId()
        }
    }
}
