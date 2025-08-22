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
    override val saksnummer: Saksnummer,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    override val fnr: Fnr,
    override val utbetaling: Utbetaling,
    val meldekortBehandling: MeldekortBehandling.Behandlet,
) : Vedtak {

    override val saksbehandler: String = meldekortBehandling.saksbehandler!!
    override val beslutter: String = meldekortBehandling.beslutter!!

    val meldekortId: MeldekortId = meldekortBehandling.id
    val automatiskBehandlet: Boolean = meldekortBehandling is MeldekortBehandletAutomatisk
    val erKorrigering: Boolean = meldekortBehandling.type == MeldekortBehandlingType.KORRIGERING
    val begrunnelse: String? = meldekortBehandling.begrunnelse?.verdi
    val rammevedtak: List<VedtakId> = meldekortBehandling.rammevedtak
    val beregningsperiode: Periode = meldekortBehandling.beregning.periode

    override val antallDagerPerMeldeperiode: Int = meldekortBehandling.meldeperiode.maksAntallDagerForMeldeperiode

    init {
        require(id == utbetaling.vedtakId)
        require(sakId == utbetaling.sakId)
        require(fnr == utbetaling.fnr)
        require(saksbehandler == utbetaling.saksbehandler)
        require(beslutter == utbetaling.beslutter)
    }
}

fun MeldekortBehandling.Behandlet.opprettVedtak(
    forrigeUtbetaling: Utbetaling?,
    clock: Clock,
): MeldekortVedtak {
    val vedtakId = VedtakId.random()

    val utbetaling = Utbetaling(
        id = UtbetalingId.random(),
        vedtakId = vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = this.opprettet,
        saksbehandler = this.saksbehandler!!,
        beslutter = this.beslutter!!,
        beregning = this.beregning,
        brukerNavkontor = this.navkontor,
        sendtTilUtbetaling = null,
        status = null,
        forrigeUtbetalingVedtakId = forrigeUtbetaling?.vedtakId,
    )

    return MeldekortVedtak(
        id = vedtakId,
        opprettet = nå(clock),
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        journalpostId = null,
        journalføringstidspunkt = null,
        meldekortBehandling = this,
        utbetaling = utbetaling,
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
