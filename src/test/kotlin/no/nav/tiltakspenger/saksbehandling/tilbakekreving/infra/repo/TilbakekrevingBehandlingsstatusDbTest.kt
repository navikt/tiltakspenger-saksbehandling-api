package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Tilbakekrevingsflyten er ikke i drift ennå, så prodstiene produserer bare de første statusene.
 * Mappingen må likevel kunne lese og skrive alle sammen, og den rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 */
class TilbakekrevingBehandlingsstatusDbTest {

    @Test
    fun `statusene lagres med sitt avtalte navn`() {
        TilbakekrevingBehandlingsstatus.entries.associateWith { it.tilDbString() } shouldBe mapOf(
            TilbakekrevingBehandlingsstatus.OPPRETTET to "OPPRETTET",
            TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL to "TIL_FORHÅNDSVARSEL",
            TilbakekrevingBehandlingsstatus.TIL_BEHANDLING to "TIL_BEHANDLING",
            TilbakekrevingBehandlingsstatus.TIL_GODKJENNING to "TIL_GODKJENNING",
            TilbakekrevingBehandlingsstatus.AVSLUTTET to "AVSLUTTET",
        )
    }

    @Test
    fun `statusene leses tilbake fra lagret verdi`() {
        TilbakekrevingBehandlingsstatus.entries.forEach {
            TilbakekrevingBehandlingsstatusDb.valueOf(it.tilDbString()).tilDomene() shouldBe it
        }
    }
}
