package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtak
import java.time.Clock
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette utbetalingsvedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 * */
data class MeldekortVedtak(
    override val id: VedtakId,
    override val opprettet: LocalDateTime,
    override val sakId: SakId,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val meldekortBehandling: MeldekortBehandling.Behandlet,
    val forrigeUtbetalingVedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: Utbetalingsstatus?,
) : Vedtak {
    val meldekortId: MeldekortId = meldekortBehandling.id
    val automatiskBehandlet: Boolean = meldekortBehandling is MeldekortBehandletAutomatisk
    val erKorrigering: Boolean = meldekortBehandling.type == MeldekortBehandlingType.KORRIGERING
    val begrunnelse: String? = meldekortBehandling.begrunnelse?.verdi
    val rammevedtak: List<VedtakId> = meldekortBehandling.rammevedtak
    val saksbehandler: String = meldekortBehandling.saksbehandler!!
    val beslutter: String = meldekortBehandling.beslutter!!
    val beregningsperiode: Periode = meldekortBehandling.beregning.periode

    override val utbetaling: Utbetaling by lazy {
        Utbetaling(
            id = UtbetalingId.random(),
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            beregning = meldekortBehandling.beregning,
            brukerNavkontor = meldekortBehandling.navkontor,
            vedtakId = id,
            forrigeUtbetalingVedtakId = forrigeUtbetalingVedtakId,
            sendtTilUtbetaling = sendtTilUtbetaling,
            status = status,
        )
    }

    override val antallDagerPerMeldeperiode: Int = meldekortBehandling.meldeperiode.maksAntallDagerForMeldeperiode

    init {
        require(id == utbetaling.vedtakId) {
            "Utbetalingen på meldekortvedtaket tilhørte et annet vedtak - forventet $id, fant ${utbetaling.vedtakId}"
        }
    }
}

fun MeldekortBehandling.Behandlet.opprettVedtak(
    saksnummer: Saksnummer,
    fnr: Fnr,
    forrigeUtbetaling: Utbetaling?,
    clock: Clock,
): MeldekortVedtak {
    val vedtakId = VedtakId.random()

    return MeldekortVedtak(
        id = vedtakId,
        opprettet = nå(clock),
        sakId = this.sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        journalpostId = null,
        journalføringstidspunkt = null,
        meldekortBehandling = this,
        forrigeUtbetalingVedtakId = forrigeUtbetaling?.vedtakId,
        sendtTilUtbetaling = null,
        status = null,
    )
}

fun MeldekortVedtak.tilStatistikk(): StatistikkUtbetalingDTO =
    StatistikkUtbetalingDTO(
        // TODO post-mvp jah: Vi sender uuid-delen av denne til helved som behandlingId som mappes videre til OS/UR i feltet 'henvisning'.
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        ordinærBeløp = this.utbetaling.ordinærBeløp,
        barnetilleggBeløp = this.utbetaling.barnetilleggBeløp,
        totalBeløp = this.utbetaling.totalBeløp,
        // TODO post-mvp jah: Vi oppretter vedtaket før og statistikken før vi sender til helved/utbetaling. Bør vi opprette statistikken etter vi har sendt til helved/utbetaling?
        posteringDato = this.opprettet.toLocalDate(),
        gyldigFraDatoPostering = this.utbetaling.periode.fraOgMed,
        gyldigTilDatoPostering = this.utbetaling.periode.tilOgMed,
        utbetalingId = this.id.uuidPart(),
        vedtakId = rammevedtak.map { it.toString() },
        opprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        brukerId = this.fnr.verdi,
    )
