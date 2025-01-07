package no.nav.tiltakspenger.utbetaling.domene

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.service.statistikk.stønad.StatistikkUtbetalingDTO
import java.time.LocalDateTime

/**
 * @property forrigeUtbetalingsvedtakId er null for første utbetalingsvedtak i en sak.
 * @param opprettet Tidspunktet vi instansierte og persisterte dette utbetalingsvedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 */
data class Utbetalingsvedtak(
    val id: VedtakId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val rammevedtakId: VedtakId,
    val opprettet: LocalDateTime,
    val meldekort: MeldekortBehandling.UtfyltMeldekort,
    val forrigeUtbetalingsvedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
) {
    val periode = meldekort.periode
    val beløpTotal = meldekort.beløpTotal
    val meldekortId = meldekort.id
    val meldeperiodeId = meldekort.meldeperiodeId
    val saksbehandler: String = meldekort.saksbehandler
    val beslutter: String = meldekort.beslutter!!
    val brukerNavkontor: Navkontor = meldekort.navkontor
}

fun MeldekortBehandling.UtfyltMeldekort.opprettUtbetalingsvedtak(
    saksnummer: Saksnummer,
    fnr: Fnr,
    forrigeUtbetalingsvedtak: VedtakId?,
): Utbetalingsvedtak =
    Utbetalingsvedtak(
        id = VedtakId.random(),
        opprettet = nå(),
        sakId = this.sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        rammevedtakId = this.rammevedtakId,
        meldekort = this,
        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtak,
        sendtTilUtbetaling = null,
        journalpostId = null,
        journalføringstidspunkt = null,
    )

fun Utbetalingsvedtak.tilStatistikk(): StatistikkUtbetalingDTO =
    StatistikkUtbetalingDTO(
        // TODO post-mvp jah: Vi sender uuid-delen av denne til helved som behandlingId som mappes videre til OS/UR i feltet 'henvisning'.
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        beløp = this.beløpTotal,
        beløpBeskrivelse = "",
        årsak = "",
        // TODO post-mvp jah: Vi oppretter vedtaket før og statistikken før vi sender til helved/utbetaling. Bør vi opprette statistikken etter vi har sendt til helved/utbetaling?
        posteringDato = this.opprettet.toLocalDate(),
        gyldigFraDatoPostering = this.periode.fraOgMed,
        gyldigTilDatoPostering = this.periode.tilOgMed,
    )
