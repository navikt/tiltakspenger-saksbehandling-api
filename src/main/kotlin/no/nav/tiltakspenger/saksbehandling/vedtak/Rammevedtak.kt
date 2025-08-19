package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.sak.utfallsperioder
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette utbetalingsvedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
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
    override val utbetaling: Utbetaling?,
    val behandling: Behandling,
    val vedtaksdato: LocalDate?,
    val vedtakstype: Vedtakstype,
    val distribusjonId: DistribusjonId?,
    val distribusjonstidspunkt: LocalDateTime?,
    val sendtTilDatadeling: LocalDateTime?,
    val brevJson: String?,
) : Vedtak,
    Periodiserbar {

    val fnr: Fnr = behandling.fnr
    val saksnummer: Saksnummer = behandling.saksnummer
    val saksbehandlerNavIdent: String = behandling.saksbehandler!!
    val beslutterNavIdent: String = behandling.beslutter!!
    val utfallsperioder: SammenhengendePeriodisering<Utfallsperiode> by lazy { behandling.utfallsperioder!! }

    /** Vil være null dersom bruker ikke har rett på barnetillegg  */
    val barnetillegg: Barnetillegg? by lazy { behandling.barnetillegg }

    val beregning: BehandlingBeregning? by lazy {
        utbetaling?.beregning as BehandlingBeregning
    }

    /** TODO jah: Endre til behandling.antallDagerPerMeldeperiode - merk at den ikke er satt for Avslag eller Stans. */
    override val antallDagerPerMeldeperiode: Int = behandling.maksDagerMedTiltakspengerForPeriode

    init {
        require(behandling.erVedtatt) { "Kan ikke lage vedtak for behandling som ikke er vedtatt. BehandlingId: ${behandling.id}" }
        require(sakId == behandling.sakId) { "SakId i vedtak og behandling må være lik. SakId: $sakId, BehandlingId: ${behandling.id}" }
        require(periode == behandling.virkningsperiode) { "Periode i vedtak og behandling må være lik. Periode: $periode, BehandlingId: ${behandling.id}" }
    }
}

enum class Vedtakstype {
    INNVILGELSE,
    AVSLAG,
    STANS,
}

fun Sak.opprettVedtak(
    behandling: Behandling,
    clock: Clock,
): Pair<Sak, Rammevedtak> {
    require(behandling.status == Behandlingsstatus.VEDTATT) { "Krever behandlingsstatus VEDTATT når vi skal opprette et vedtak." }

    val vedtakId = VedtakId.random()

    val utbetaling = behandling.utbetaling?.let {
        Utbetaling(
            beregning = it.beregning,
            brukerNavkontor = it.navkontor,
            vedtakId = vedtakId,
            forrigeUtbetalingVedtakId = utbetalinger.lastOrNull()?.vedtakId,
            sendtTilUtbetaling = null,
            status = null,
        )
    }

    val vedtak = Rammevedtak(
        id = vedtakId,
        opprettet = nå(clock),
        sakId = this.id,
        behandling = behandling,
        vedtaksdato = null,
        vedtakstype = this.utledVedtakstype(behandling),
        periode = behandling.virkningsperiode!!,
        journalpostId = null,
        journalføringstidspunkt = null,
        distribusjonId = null,
        distribusjonstidspunkt = null,
        sendtTilDatadeling = null,
        brevJson = null,
        utbetaling = utbetaling,
    )

    val oppdatertSak = this.copy(vedtaksliste = this.vedtaksliste.leggTilVedtak(vedtak))

    return oppdatertSak to vedtak
}

fun Sak.utledVedtakstype(behandling: Behandling): Vedtakstype {
    return when (behandling) {
        is Søknadsbehandling -> {
            when (behandling.resultat) {
                is SøknadsbehandlingResultat.Avslag -> Vedtakstype.AVSLAG
                is SøknadsbehandlingResultat.Innvilgelse -> Vedtakstype.INNVILGELSE
                null -> throw IllegalArgumentException("Kan ikke lage et vedtak uten resultat. Behandlingen uten resultat er ${behandling.id}")
            }
        }

        is Revurdering -> {
            when (behandling.resultat) {
                is RevurderingResultat.Innvilgelse -> Vedtakstype.INNVILGELSE
                is RevurderingResultat.Stans -> {
                    val revurderingTilOgmed = behandling.virkningsperiode!!.tilOgMed
                    val sakTilOgMed = this.utfallsperioder().totalPeriode.tilOgMed
                    val sisteUtbetalteMeldekortDag = this.sisteUtbetalteMeldekortDag()

                    check(revurderingTilOgmed == sakTilOgMed) {
                        "Kan ikke lage stansvedtak for revurdering - revurderingens tilOgMed ($revurderingTilOgmed) må være lik sakens tilOgMed ($sakTilOgMed)"
                    }

                    check(sisteUtbetalteMeldekortDag == null || sisteUtbetalteMeldekortDag < behandling.virkningsperiode.fraOgMed) {
                        "Kan ikke lage stansvedtak for revurdering - godkjent meldekort overlapper revurderingsperioden"
                    }

                    Vedtakstype.STANS
                }
            }
        }
    }
}
