package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.sak.utfallsperioder
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param opprettet Tidspunktet vi instansierte og persisterte dette utbetalingsvedtaket første gangen. Dette har ingenting med vedtaksbrevet å gjøre.
 * @periode Stansperiode eller innvilgelsesperiode (ikke nødvendigvis det samme som vurderingsperiode.)
 */
data class Rammevedtak(
    override val id: VedtakId = VedtakId.random(),
    override val opprettet: LocalDateTime,
    val sakId: SakId,
    val behandling: Behandling,
    val vedtaksdato: LocalDate?,
    val vedtaksType: Vedtakstype,
    override val periode: Periode,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
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
    val utfallsperioder: Periodisering<Utfallsperiode>? by lazy { behandling.utfallsperioder }

    /** Vil være null dersom ingen barn. */
    val barnetillegg: Barnetillegg? by lazy { behandling.barnetillegg }
    override val antallDagerPerMeldeperiode: Int = behandling.maksDagerMedTiltakspengerForPeriode

    override fun erStansvedtak(): Boolean {
        return vedtaksType == Vedtakstype.STANS
    }

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

    val vedtak = Rammevedtak(
        id = VedtakId.random(),
        opprettet = nå(clock),
        sakId = this.id,
        behandling = behandling,
        vedtaksdato = null,
        vedtaksType = this.utledVedtakstype(behandling),
        periode = behandling.virkningsperiode!!,
        journalpostId = null,
        journalføringstidspunkt = null,
        distribusjonId = null,
        distribusjonstidspunkt = null,
        sendtTilDatadeling = null,
        brevJson = null,
    )

    val oppdatertSak = this.copy(vedtaksliste = this.vedtaksliste.leggTilFørstegangsVedtak(vedtak))

    return oppdatertSak to vedtak
}

fun Sak.utledVedtakstype(behandling: Behandling): Vedtakstype {
    return when (behandling) {
        is Søknadsbehandling -> {
            when (behandling.utfall) {
                is SøknadsbehandlingResultat.Avslag -> Vedtakstype.AVSLAG
                is SøknadsbehandlingResultat.Innvilgelse -> Vedtakstype.INNVILGELSE
                null -> throw IllegalArgumentException("Kan ikke lage et vedtak uten utfall. Behandlingen uten utfall er ${behandling.id}")
            }
        }

        is Revurdering -> {
            when (behandling.utfall) {
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
