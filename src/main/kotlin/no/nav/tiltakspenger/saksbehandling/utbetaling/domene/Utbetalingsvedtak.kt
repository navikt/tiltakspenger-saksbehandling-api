package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingsberegning
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkUtbetalingDTO
import java.time.Clock
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
    val opprettet: LocalDateTime,
    val forrigeUtbetalingsvedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val status: Utbetalingsstatus?,
    val beregning: Utbetalingsberegning,
    val saksbehandler: String,
    val beslutter: String,
    val brukerNavkontor: Navkontor,
    val rammevedtak: List<VedtakId>,
    val automatiskBehandlet: Boolean,
) {
    val periode: Periode = beregning.periode
    val ordinærBeløp: Int = beregning.ordinærBeløp
    val barnetilleggBeløp: Int = beregning.barnetilleggBeløp
    val totalBeløp: Int = beregning.totalBeløp
    val beregningKilde: BeregningKilde = beregning.beregningKilde

    fun oppdaterStatus(status: Utbetalingsstatus?): Utbetalingsvedtak {
        return this.copy(status = status)
    }
}

fun MeldekortBehandling.Behandlet.opprettUtbetalingsvedtak(
    saksnummer: Saksnummer,
    fnr: Fnr,
    forrigeUtbetalingsvedtak: Utbetalingsvedtak?,
    clock: Clock,
): Utbetalingsvedtak =
    Utbetalingsvedtak(
        id = VedtakId.random(),
        opprettet = nå(clock),
        sakId = this.sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtak?.id,
        sendtTilUtbetaling = null,
        journalpostId = null,
        journalføringstidspunkt = null,
        status = null,
        beregning = this.beregning,
        saksbehandler = this.saksbehandler!!,
        beslutter = this.beslutter!!,
        brukerNavkontor = this.navkontor,
        rammevedtak = this.rammevedtak!!,
        automatiskBehandlet = this is MeldekortBehandletAutomatisk,
    )

fun Utbetalingsvedtak.tilStatistikk(): StatistikkUtbetalingDTO =
    StatistikkUtbetalingDTO(
        // TODO post-mvp jah: Vi sender uuid-delen av denne til helved som behandlingId som mappes videre til OS/UR i feltet 'henvisning'.
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        ordinærBeløp = this.ordinærBeløp,
        barnetilleggBeløp = this.barnetilleggBeløp,
        totalBeløp = this.totalBeløp,
        // TODO post-mvp jah: Vi oppretter vedtaket før og statistikken før vi sender til helved/utbetaling. Bør vi opprette statistikken etter vi har sendt til helved/utbetaling?
        posteringDato = this.opprettet.toLocalDate(),
        gyldigFraDatoPostering = this.periode.fraOgMed,
        gyldigTilDatoPostering = this.periode.tilOgMed,
        utbetalingId = this.id.uuidPart(),
        vedtakId = rammevedtak.map { it.toString() },
        opprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        brukerId = this.fnr.verdi,
    )
