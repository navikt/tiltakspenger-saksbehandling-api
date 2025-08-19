package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import java.time.LocalDateTime

/**
 * Kommentar jah: Denne vil ikke passe for klagevedtak og tilbakekrevingsvedtak, da må vi lage et abstraksjonslag til.
 */
interface Vedtak {
    val id: VedtakId
    val sakId: SakId
    val opprettet: LocalDateTime
    val journalpostId: JournalpostId?
    val journalføringstidspunkt: LocalDateTime?
    val utbetaling: Utbetaling?

    // TODO post-mvp jah: Dette er nok en forenkling, siden må kunne anta at antallDagerForMeldeperiode forandrer seg innenfor et vedtak.
    val antallDagerPerMeldeperiode: Int
}
