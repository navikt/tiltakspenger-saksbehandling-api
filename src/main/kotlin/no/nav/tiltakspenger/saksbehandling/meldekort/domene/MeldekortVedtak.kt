package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtak
import no.nav.utsjekk.kontrakter.felles.Satstype
import java.time.Clock
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette vedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 * */
data class MeldekortVedtak(
    override val id: VedtakId,
    override val opprettet: LocalDateTime,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    override val fnr: Fnr,
    override val utbetaling: VedtattUtbetaling,
    val meldekortBehandling: MeldekortBehandling.Behandlet,
) : Vedtak,
    Periodiserbar {

    override val saksbehandler: String = meldekortBehandling.saksbehandler!!
    override val beslutter: String = meldekortBehandling.beslutter!!

    val meldekortId: MeldekortId = meldekortBehandling.id
    val automatiskBehandlet: Boolean = meldekortBehandling is MeldekortBehandletAutomatisk
    val erKorrigering: Boolean = meldekortBehandling.type == MeldekortBehandlingType.KORRIGERING
    val begrunnelse: String? = meldekortBehandling.begrunnelse?.verdi
    val rammevedtak: List<VedtakId> = meldekortBehandling.rammevedtak
    val beregningsperiode: Periode = meldekortBehandling.beregning.periode
    override val periode: Periode = beregningsperiode

    val antallDagerPerMeldeperiode: Int = meldekortBehandling.meldeperiode.maksAntallDagerForMeldeperiode

    init {
        require(id == utbetaling.vedtakId)
        require(sakId == utbetaling.sakId)
        require(fnr == utbetaling.fnr)
        require(opprettet == utbetaling.opprettet)
        require(saksbehandler == utbetaling.saksbehandler)
        require(beslutter == utbetaling.beslutter)
        require(meldekortBehandling.id == utbetaling.beregningKilde.id)
        require(meldekortBehandling.beregning == utbetaling.beregning)
    }
}

fun MeldekortBehandling.Behandlet.opprettVedtak(
    forrigeUtbetaling: VedtattUtbetaling?,
    kanUtbetaleHelgPåFredag: Boolean,
    clock: Clock,
): MeldekortVedtak {
    val vedtakId = VedtakId.random()
    val opprettet = nå(clock)

    val utbetaling = VedtattUtbetaling(
        id = UtbetalingId.random(),
        vedtakId = vedtakId,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = opprettet,
        saksbehandler = this.saksbehandler!!,
        beslutter = this.beslutter!!,
        beregning = this.beregning,
        brukerNavkontor = this.navkontor,
        forrigeUtbetalingId = forrigeUtbetaling?.id,
        statusMetadata = Forsøkshistorikk.opprett(clock = clock),
        satstype = Satstype.DAGLIG,
        kanUtbetaleHelgPåFredag = kanUtbetaleHelgPåFredag,
        sendtTilUtbetaling = null,
        status = null,
    )

    return MeldekortVedtak(
        id = vedtakId,
        opprettet = opprettet,
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        journalpostId = null,
        journalføringstidspunkt = null,
        meldekortBehandling = this,
        utbetaling = utbetaling,
    )
}
