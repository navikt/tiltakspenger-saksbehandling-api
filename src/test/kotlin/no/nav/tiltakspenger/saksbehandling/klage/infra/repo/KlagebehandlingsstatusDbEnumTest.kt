package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Statusene etter oversending til klageinstansen krever svar utenfra, så flere av dem ville kostet en konstruert flyt per variant.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 */
class KlagebehandlingsstatusDbEnumTest {

    @Test
    fun `statusene lagres med sitt avtalte navn`() {
        Klagebehandlingsstatus.entries.associateWith { it.toDbEnum() } shouldBe mapOf(
            Klagebehandlingsstatus.KLAR_TIL_BEHANDLING to "KLAR_TIL_BEHANDLING",
            Klagebehandlingsstatus.UNDER_BEHANDLING to "UNDER_BEHANDLING",
            Klagebehandlingsstatus.AVBRUTT to "AVBRUTT",
            Klagebehandlingsstatus.VEDTATT to "VEDTATT",
            Klagebehandlingsstatus.OPPRETTHOLDT to "OPPRETTHOLDT",
            Klagebehandlingsstatus.OVERSENDT to "OVERSENDT",
            Klagebehandlingsstatus.OVERSEND_FEILET to "OVERSEND_FEILET",
            Klagebehandlingsstatus.FERDIGSTILT to "FERDIGSTILT",
            Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS to "MOTTATT_FRA_KLAGEINSTANS",
            Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS to "OMGJØRING_ETTER_KLAGEINSTANS",
        )
    }

    @Test
    fun `statusene leses tilbake fra lagret verdi`() {
        Klagebehandlingsstatus.entries.forEach {
            it.toDbEnum().toKlagebehandlingsstatus() shouldBe it
        }
    }
}
