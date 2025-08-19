package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkUtbetalingDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
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
    override val utbetaling: Utbetaling,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val meldekortBehandling: MeldekortBehandling.Behandlet,
) : Vedtak {
    val meldekortId: MeldekortId = meldekortBehandling.id
    val automatiskBehandlet: Boolean = meldekortBehandling is MeldekortBehandletAutomatisk
    val erKorrigering: Boolean = meldekortBehandling.type == MeldekortBehandlingType.KORRIGERING
    val begrunnelse: String? = meldekortBehandling.begrunnelse?.verdi
    val rammevedtak: List<VedtakId> = meldekortBehandling.rammevedtak
    val brukerNavkontor: Navkontor = meldekortBehandling.navkontor
    val saksbehandler: String = meldekortBehandling.saksbehandler!!
    val beslutter: String = meldekortBehandling.beslutter!!
    val beregningsperiode: Periode = utbetaling.periode

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
        utbetaling = Utbetaling(
            beregning = this.beregning,
            brukerNavkontor = this.navkontor,
            vedtakId = vedtakId,
            forrigeUtbetalingVedtakId = forrigeUtbetaling?.vedtakId,
            sendtTilUtbetaling = null,
            status = null,
        ),
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
