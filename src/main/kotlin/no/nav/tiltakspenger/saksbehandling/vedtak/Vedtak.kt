package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.LocalDateTime

/**
 * @param beslutter Vil være null for alle klagebehandlinger.
 * @param beregning Vil være null for alle klagebehandlinger og rammevedtak uten utbetaling.
 * @param utbetaling Vil være null for alle klagebehandlinger og rammevedtak uten utbetaling.
 */
interface Vedtak {
    val id: VedtakId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val opprettet: LocalDateTime
    val saksbehandler: String
    val beslutter: String?
    val journalpostId: JournalpostId?
    val journalføringstidspunkt: LocalDateTime?
    val beregning: Beregning?
    val utbetaling: VedtattUtbetaling?
}
