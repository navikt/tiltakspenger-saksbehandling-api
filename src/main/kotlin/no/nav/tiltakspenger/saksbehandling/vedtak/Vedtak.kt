package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDateTime

/**
 * Kommentar jah: Denne vil ikke passe for klagevedtak og tilbakekrevingsvedtak, da må vi lage et abstraksjonslag til.
 */
interface Vedtak {
    val id: VedtakId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val opprettet: LocalDateTime
    val saksbehandler: String
    val beslutter: String
    val journalpostId: JournalpostId?
    val journalføringstidspunkt: LocalDateTime?
    val utbetaling: VedtattUtbetaling?

    // TODO post-mvp jah: Dette er nok en forenkling, siden må kunne anta at antallDagerForMeldeperiode forandrer seg innenfor et vedtak.
    val antallDagerPerMeldeperiode: Int
}
