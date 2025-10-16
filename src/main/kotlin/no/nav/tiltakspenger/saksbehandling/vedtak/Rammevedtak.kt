package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.utsjekk.kontrakter.felles.Satstype
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette vedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 * @param periode Stansperiode eller innvilgelsesperiode (ikke nødvendigvis det samme som vurderingsperiode.)
 * @param vedtaksdato Datoen vi bruker i brevet. Lagres samtidig som vi genererer og journalfører brevet. Vil være null fram til dette.
 */
data class Rammevedtak(
    override val id: VedtakId = VedtakId.random(),
    override val opprettet: LocalDateTime,
    override val sakId: SakId,
    override val periode: Periode,
    override val journalpostId: JournalpostId?,
    override val journalføringstidspunkt: LocalDateTime?,
    override val utbetaling: VedtattUtbetaling?,
    val behandling: Rammebehandling,
    val vedtaksdato: LocalDate?,
    val vedtakstype: Vedtakstype,
    val distribusjonId: DistribusjonId?,
    val distribusjonstidspunkt: LocalDateTime?,
    val sendtTilDatadeling: LocalDateTime?,
    val brevJson: String?,
) : Vedtak,
    Periodiserbar {

    override val fnr: Fnr = behandling.fnr
    override val saksnummer: Saksnummer = behandling.saksnummer
    override val saksbehandler: String = behandling.saksbehandler!!
    override val beslutter: String = behandling.beslutter!!

    /** Vil være null dersom bruker ikke har rett på barnetillegg  */
    val barnetillegg: Barnetillegg? by lazy { behandling.barnetillegg }

    /** Vil være null for stans eller avslag */
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>? =
        behandling.antallDagerPerMeldeperiode

    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = behandling.valgteTiltaksdeltakelser

    init {
        require(behandling.erVedtatt) { "Kan ikke lage vedtak for behandling som ikke er vedtatt. BehandlingId: ${behandling.id}" }
        require(sakId == behandling.sakId) { "SakId i vedtak og behandling må være lik. SakId: $sakId, BehandlingId: ${behandling.id}" }
        require(periode == behandling.virkningsperiode) { "Periode i vedtak ($periode) og behandlingens virkningsperiode (${behandling.virkningsperiode}) må være lik. SakId: $sakId, Saksnummer: ${behandling.saksnummer} BehandlingId: ${behandling.id}" }

        utbetaling?.also {
            require(id == it.vedtakId)
            require(sakId == it.sakId)
            require(fnr == it.fnr)
            require(opprettet == it.opprettet)
            require(saksbehandler == it.saksbehandler)
            require(beslutter == it.beslutter)
            require(behandling.id == it.beregningKilde.id)
        }
    }
}

enum class Vedtakstype {
    INNVILGELSE,
    AVSLAG,
    STANS,
}

fun Sak.opprettVedtak(
    behandling: Rammebehandling,
    clock: Clock,
): Pair<Sak, Rammevedtak> {
    require(behandling.status == Rammebehandlingsstatus.VEDTATT) { "Krever behandlingsstatus VEDTATT når vi skal opprette et vedtak." }

    val vedtakId = VedtakId.random()
    val opprettet = nå(clock)

    val utbetaling: VedtattUtbetaling? = behandling.utbetaling?.let {
        VedtattUtbetaling(
            id = UtbetalingId.random(),
            vedtakId = vedtakId,
            sakId = this.id,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            brukerNavkontor = it.navkontor,
            opprettet = opprettet,
            saksbehandler = behandling.saksbehandler!!,
            beslutter = behandling.beslutter!!,
            beregning = it.beregning,
            forrigeUtbetalingId = this.utbetalinger.lastOrNull()?.id,
            statusMetadata = Forsøkshistorikk.opprett(clock = clock),
            satstype = Satstype.DAGLIG,
            sendtTilUtbetaling = null,
            status = null,
        )
    }

    val vedtak = Rammevedtak(
        id = vedtakId,
        opprettet = opprettet,
        sakId = this.id,
        behandling = behandling,
        vedtaksdato = null,
        vedtakstype = when (behandling.resultat!!) {
            is SøknadsbehandlingResultat.Avslag -> Vedtakstype.AVSLAG
            is SøknadsbehandlingResultat.Innvilgelse -> Vedtakstype.INNVILGELSE
            is RevurderingResultat.Innvilgelse -> Vedtakstype.INNVILGELSE
            is RevurderingResultat.Stans -> Vedtakstype.STANS
        },
        periode = behandling.virkningsperiode!!,
        journalpostId = null,
        journalføringstidspunkt = null,
        distribusjonId = null,
        distribusjonstidspunkt = null,
        sendtTilDatadeling = null,
        brevJson = null,
        utbetaling = utbetaling,
    )

    val oppdatertSak = this.leggTilRammevedtak(vedtak)

    return oppdatertSak to vedtak
}
