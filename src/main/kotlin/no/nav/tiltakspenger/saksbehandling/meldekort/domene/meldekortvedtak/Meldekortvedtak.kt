package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
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
    val meldekortbehandling: Meldekortbehandling.Behandlet,
) : Vedtak,
    Periodiserbar {
    override val behandlingId: MeldekortId = meldekortbehandling.id
    override val saksbehandler: String = meldekortbehandling.saksbehandler!!
    override val beslutter: String = meldekortbehandling.beslutter!!

    override val beregning: Beregning = meldekortbehandling.beregning

    val meldeperiode: Meldeperiode = meldekortbehandling.meldeperiode

    val meldekortId: MeldekortId = meldekortbehandling.id
    val automatiskBehandlet: Boolean = meldekortbehandling is MeldekortBehandletAutomatisk
    val erKorrigering: Boolean = meldekortbehandling.type == MeldekortbehandlingType.KORRIGERING
    val begrunnelse: String? = meldekortbehandling.begrunnelse?.verdi
    val rammevedtak: List<VedtakId> = meldekortbehandling.rammevedtak
    val beregningsperiode: Periode = meldekortbehandling.beregning.periode
    val antallDagerPerMeldeperiode: Int = meldeperiode.maksAntallDagerForMeldeperiode
    val dager: List<MeldekortDag> = meldekortbehandling.dager
    val kjedeId: MeldeperiodeKjedeId = meldekortbehandling.kjedeId
    val skalSendeVedtaksbrev: Boolean = meldekortbehandling.skalSendeVedtaksbrev

    override val periode: Periode = meldeperiode.periode

    init {
        require(id == utbetaling.vedtakId)
        require(sakId == utbetaling.sakId)
        require(fnr == utbetaling.fnr)
        require(opprettet == utbetaling.opprettet)
        require(saksbehandler == utbetaling.saksbehandler)
        require(beslutter == utbetaling.beslutter)
        require(meldekortbehandling.id == utbetaling.beregningKilde.id)
        require(meldekortbehandling.beregning == utbetaling.beregning)
    }
}

fun Meldekortbehandling.Behandlet.opprettVedtak(
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
        meldekortbehandling = this,
        utbetaling = utbetaling,
    )
}
