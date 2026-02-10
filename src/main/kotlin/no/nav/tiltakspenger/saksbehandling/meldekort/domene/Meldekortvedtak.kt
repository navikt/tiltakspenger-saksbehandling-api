package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtak
import java.time.Clock
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette vedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 * */
data class Meldekortvedtak(
    override val id: VedtakId,
    override val opprettet: LocalDateTime,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    override val fnr: Fnr,
    override val utbetaling: VedtattUtbetaling,
    val meldekortBehandling: MeldekortBehandling.Behandlet,
) : Vedtak {

    override val saksbehandler: String = meldekortBehandling.saksbehandler!!
    override val beslutter: String = meldekortBehandling.beslutter!!

    override val beregning: Beregning = meldekortBehandling.beregning

    val meldeperiode: Meldeperiode = meldekortBehandling.meldeperiode

    val meldekortId: MeldekortId = meldekortBehandling.id
    val automatiskBehandlet: Boolean = meldekortBehandling is MeldekortBehandletAutomatisk
    val erKorrigering: Boolean = meldekortBehandling.type == MeldekortBehandlingType.KORRIGERING
    val begrunnelse: String? = meldekortBehandling.begrunnelse?.verdi
    val rammevedtak: List<VedtakId> = meldekortBehandling.rammevedtak
    val beregningsperiode: Periode = meldekortBehandling.beregning.periode
    val periode: Periode = meldeperiode.periode

    val antallDagerPerMeldeperiode: Int = meldeperiode.maksAntallDagerForMeldeperiode

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
    clock: Clock,
): Meldekortvedtak {
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
        sendtTilUtbetaling = null,
        status = null,
    )

    return Meldekortvedtak(
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
