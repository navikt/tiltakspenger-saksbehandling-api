package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad.StatistikkUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
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
    val meldekortbehandling: MeldekortBehandling.MeldekortBehandlet,
    val forrigeUtbetalingsvedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val status: Utbetalingsstatus?,
) {
    val periode: Periode = meldekortbehandling.periode
    val ordinærBeløp: Int = meldekortbehandling.ordinærBeløp
    val barnetilleggBeløp: Int = meldekortbehandling.barnetilleggBeløp
    val totalBeløp: Int = meldekortbehandling.beløpTotal
    val meldekortId: MeldekortId = meldekortbehandling.id
    val kjedeId: MeldeperiodeKjedeId = meldekortbehandling.kjedeId
    val saksbehandler: String = meldekortbehandling.saksbehandler
    val beslutter: String = meldekortbehandling.beslutter!!
    val brukerNavkontor: Navkontor = meldekortbehandling.navkontor
    val meldeperiode: Meldeperiode = meldekortbehandling.meldeperiode

    fun oppdaterStatus(status: Utbetalingsstatus?): Utbetalingsvedtak {
        return this.copy(status = status)
    }
}

fun MeldekortBehandling.MeldekortBehandlet.opprettUtbetalingsvedtak(
    saksnummer: Saksnummer,
    fnr: Fnr,
    forrigeUtbetalingsvedtak: VedtakId?,
    clock: Clock,
): Utbetalingsvedtak =
    Utbetalingsvedtak(
        id = VedtakId.random(),
        opprettet = nå(clock),
        sakId = this.sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        meldekortbehandling = this,
        forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtak,
        sendtTilUtbetaling = null,
        journalpostId = null,
        journalføringstidspunkt = null,
        status = null,
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
        årsak = "",
        // TODO post-mvp jah: Vi oppretter vedtaket før og statistikken før vi sender til helved/utbetaling. Bør vi opprette statistikken etter vi har sendt til helved/utbetaling?
        posteringDato = this.opprettet.toLocalDate(),
        gyldigFraDatoPostering = this.periode.fraOgMed,
        gyldigTilDatoPostering = this.periode.tilOgMed,
        utbetalingId = this.id.uuidPart(),
    )
