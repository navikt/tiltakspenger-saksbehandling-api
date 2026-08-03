package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock
import java.time.LocalDateTime

const val DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE: Int = 10

/**
 * En rammebehandling fører til et [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak].
 * Dette er en vurderingen av søknaden og inngangsvilkårene - om en bruker har rett til tiltangspenger i en gitt periode.
 * Dette gjelder både søknadsbehandling (innvilgelse og avslag) og revurdering (endring og omgjøring, inkl. stans/opphør, innvilgelse/forlengelse)
 * Se [no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling] for behandling av meldekort innenfor et [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak].
 */
sealed interface Rammebehandling : AttesterbarBehandling {

    override val id: RammebehandlingId
    val status: Rammebehandlingsstatus
    override val opprettet: LocalDateTime

    val sistEndret: LocalDateTime
    override val iverksattTidspunkt: LocalDateTime?
    val sendtTilDatadeling: LocalDateTime?
    override val sakId: SakId

    override val saksnummer: Saksnummer
    override val fnr: Fnr
    val saksopplysninger: Saksopplysninger

    override val saksbehandler: String?
    override val beslutter: String?
    override val sendtTilBeslutning: LocalDateTime?
    override val attesteringer: Attesteringer

    val avbrutt: Avbrutt?
    val ventestatus: Ventestatus
    val venterTil: LocalDateTime?
    val resultat: Rammebehandlingsresultat?
    val vedtaksperiode: Periode?
    val innvilgelsesperioder: Innvilgelsesperioder?
    val begrunnelseVilkårsvurdering: Begrunnelse?

    val valgteTiltaksdeltakelser: IkkeTomPeriodisering<Tiltaksdeltakelse>?
    val barnetillegg: Barnetillegg?

    val antallDagerPerMeldeperiode: IkkeTomPeriodisering<AntallDagerForMeldeperiode>?

    val behandlingstype: Behandlingstype
        get() = when (this) {
            is Revurdering -> Behandlingstype.REVURDERING
            is Søknadsbehandling -> Behandlingstype.SØKNADSBEHANDLING
        }

    val erKlarTilBehandling: Boolean get() = status == KLAR_TIL_BEHANDLING
    val erUnderAutomatiskBehandling: Boolean get() = status == UNDER_AUTOMATISK_BEHANDLING
    val erUnderBehandling: Boolean get() = status == UNDER_BEHANDLING || status == UNDER_AUTOMATISK_BEHANDLING
    val erKlarTIlBeslutning: Boolean get() = status == KLAR_TIL_BESLUTNING
    val erUnderBeslutning: Boolean get() = status == UNDER_BESLUTNING
    val erUnderBehandlingEllerBeslutning: Boolean get() = erUnderBehandling || erUnderBeslutning
    val erUnderAktivBehandling: Boolean get() = erKlarTilBehandling || erUnderBehandling || erKlarTIlBeslutning || erUnderBeslutning || erUnderkjent

    override val erAvbrutt: Boolean get() = status == AVBRUTT
    val erVedtatt: Boolean get() = status == VEDTATT
    override val erAvsluttet: Boolean get() = erAvbrutt || erVedtatt
    val erUnderkjent: Boolean get() = attesteringer.erUnderkjent()
    val saksopplysningsperiode: Periode? get() = saksopplysninger.periode

    val omgjørRammevedtak: OmgjørRammevedtak

    val utbetaling: BehandlingUtbetaling?
    val utbetalingskontroll: Utbetalingskontroll?

    override val klagebehandling: Klagebehandling?

    val skalSendeVedtaksbrev: Boolean

    fun getTiltaksdeltakelse(internDeltakelseId: TiltaksdeltakerId): Tiltaksdeltakelse? =
        saksopplysninger.getTiltaksdeltakelse(internDeltakelseId)

    fun erFerdigutfylt(): Boolean

    fun init() {
        if (beslutter != null && saksbehandler != null) {
            require(beslutter != saksbehandler) {
                "Saksbehandler og beslutter kan ikke være samme person. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
            }
        }
        when (status) {
            UNDER_AUTOMATISK_BEHANDLING -> {
                require(saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID) {
                    "Behandlingen må være tildelt $AUTOMATISK_SAKSBEHANDLER_ID når statusen er UNDER_AUTOMATISK_BEHANDLING. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                }
                require(iverksattTidspunkt == null)
                require(beslutter == null)
                require(this is Søknadsbehandling) {
                    "Kun søknadsbehandlinger kan være under automatisk behandling. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                }
                require(klagebehandling == null) {
                    "Klagebehandling kan ikke være knyttet til en behandling som er under automatisk behandling. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                }
            }

            KLAR_TIL_BEHANDLING -> {
                require(saksbehandler == null) {
                    "Behandlingen kan ikke være tilknyttet en saksbehandler når statusen er KLAR_TIL_BEHANDLING"
                }
                require(iverksattTidspunkt == null)

                if (this is Revurdering || (this is Søknadsbehandling && attesteringer.isEmpty())) {
                    require(beslutter == null) { "Beslutter kan ikke være tilknyttet behandlingen dersom det ikke er en underkjent automatisk behandlet søknadsbehandling" }
                }
                if (klagebehandling != null) {
                    require(klagebehandling!!.status == Klagebehandlingsstatus.KLAR_TIL_BEHANDLING || klagebehandling!!.erFerdigstilt) {
                        "Klagebehandling knyttet til en rammebehandling som er KLAR_TIL_BEHANDLING må ha status KLAR_TIL_BEHANDLING/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                    }
                }
            }

            UNDER_BEHANDLING -> {
                requireNotNull(saksbehandler) {
                    "Behandlingen må være tilknyttet en saksbehandler når status er UNDER_BEHANDLING"
                }
                // Selvom beslutter har underkjent, må vi kunne ta hen av behandlingen.
                require(iverksattTidspunkt == null)
                if (attesteringer.isEmpty()) {
                    require(beslutter == null) { "Beslutter kan ikke være tilknyttet behandlingen dersom det ikke er gjort noen attesteringer" }
                }
                // Vi kan ikke kreve at resultatet er satt dersom den har vært underkjent, siden hentOpplysninger kan resette saksoplysninger og implisitt resultatet.
                if (klagebehandling != null) {
                    require(
                        (klagebehandling!!.erOmgjøring && (klagebehandling!!.erUnderBehandling || klagebehandling!!.erFerdigstilt)) ||
                            (klagebehandling!!.erOpprettholdt && (klagebehandling!!.omgjørEtterKA || klagebehandling!!.erFerdigstilt)),
                    ) {
                        "Klagebehandling knyttet til en rammebehandling som er UNDER_BEHANDLING må ha status UNDER_BEHANDLING/OMGJØRING_ETTER_KLAGEINSTANS, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                    }
                }
            }

            KLAR_TIL_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er KLAR_TIL_BESLUTNING" }
                require(beslutter == null) {
                    "Behandlingen kan ikke være tilknyttet en beslutter når status er KLAR_TIL_BESLUTNING"
                }
                require(iverksattTidspunkt == null)
                require(vedtaksperiode != null) { "Vedtaksperiode må være satt for statusen KLAR_TIL_BESLUTNING" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen KLAR_TIL_BESLUTNING" }
                require(erFerdigutfylt())
                if (klagebehandling != null) {
                    require(klagebehandling!!.erUnderBehandling || klagebehandling!!.omgjørEtterKA || klagebehandling!!.erFerdigstilt || klagebehandling!!.erKlarTilBehandling) {
                        "Klagebehandling knyttet til en rammebehandling som er KLAR_TIL_BESLUTNING må ha status UNDER_BEHANDLING/KLAR_TIL_BEHANDLING/OMGJØRING_ETTER_KLAGEINSTANS/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                    }
                }
            }

            UNDER_BESLUTNING -> {
                // Vi kan ikke ta saksbehandler av behandlingen før den underkjennes.
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er UNDER_BESLUTNING" }
                requireNotNull(beslutter) { "Behandlingen må tilknyttet en beslutter når status er UNDER_BESLUTNING" }
                require(iverksattTidspunkt == null)
                require(vedtaksperiode != null) { "Vedtaksperiode må være satt for statusen UNDER_BESLUTNING" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen UNDER_BESLUTNING" }
                require(erFerdigutfylt())
                if (klagebehandling != null) {
                    require(klagebehandling!!.erUnderBehandling || klagebehandling!!.omgjørEtterKA || klagebehandling!!.erFerdigstilt) {
                        "Klagebehandling knyttet til en rammebehandling som er UNDER_BESLUTNING må ha status UNDER_BEHANDLING/FERDIGSITLT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                    }
                }
            }

            VEDTATT -> {
                // Det er viktig at vi ikke tar saksbehandler og beslutter av behandlingen når status er VEDTATT.
                requireNotNull(beslutter) { "Behandlingen må ha beslutter når status er VEDTATT" }
                requireNotNull(saksbehandler) { "Behandlingen må ha saksbehandler når status er VEDTATT" }
                requireNotNull(iverksattTidspunkt)
                requireNotNull(sendtTilBeslutning)
                require(vedtaksperiode != null) { "Vedtaksperiode må være satt for statusen VEDTATT" }
                require(this.resultat != null) { "Behandlingsresultat må være satt for statusen VEDTATT" }
                require(erFerdigutfylt())
                if (klagebehandling != null) {
                    require(klagebehandling!!.erVedtatt || klagebehandling!!.erFerdigstilt) {
                        "Klagebehandling knyttet til en rammebehandling som er VEDTATT må ha status VEDTATT/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                    }
                }
            }

            AVBRUTT -> {
                requireNotNull(avbrutt)
                require(klagebehandling?.behandlingId?.contains(id) != true) {
                    // Merk at vi beholder koblingen til klagebehandlingen ved avbrutt rammebehandling for historikkens skyld (men ikke omvendt).
                    // Hvis dette biter oss senere, kan vi fjerne koblingen.
                    "En klagebehandling skal ikke peke på en avbrutt rammebehandling. I de tilfellene ønsker vi nok et annet resultat på klagebehandlingen, eller å knytte den til en ny rammebehandling. sakId: ${this.sakId}, saksnummer: ${this.saksnummer}, rammebehandlingId: ${this.id}, klagebehandlingId: ${this.klagebehandling?.id}"
                }
            }
        }
        if (klagebehandling != null) {
            require(fnr == klagebehandling!!.fnr) {
                "Klagebehandlingens fnr må være lik behandlingens fnr. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
            }
            require(sakId == klagebehandling!!.sakId) {
                "Klagebehandlingens sakId må være lik behandlingens sakId. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
            }
            require(saksnummer == klagebehandling!!.saksnummer) {
                "Klagebehandlingens saksnummer må være lik behandlingens saksnummer. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
            }
            if (!this.erAvbrutt) {
                require(klagebehandling!!.behandlingId.contains(this.id)) {
                    "Klagebehandlingens behandlingId må inneholde behandlingens id. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
                }
            }
        }
        if (klagebehandling != null && !erAvbrutt && !klagebehandling!!.erFerdigstilt && !klagebehandling!!.erKlarTilBehandling) {
            require(saksbehandler == klagebehandling!!.saksbehandler) {
                "Klagebehandlingens saksbehandler må være lik behandlingens saksbehandler. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
            }
            require(klagebehandling!!.resultat is Klagebehandlingsresultat.Omgjør || klagebehandling!!.resultat is Klagebehandlingsresultat.Opprettholdt) {
                "Klagebehandlingens resultat må være Omgjør/Opprettholdt når den er knyttet til en rammebehandling som ikke er avbrutt. sakId: $sakId, saksnummer: $saksnummer, rammebehandlingId: $id, klagebehandlingId: ${klagebehandling?.id}"
            }
        }
    }

    /** Oppdaterer beregning og simulering for utbetaling */
    fun oppdaterUtbetaling(oppdatertUtbetaling: BehandlingUtbetaling?, clock: Clock): Rammebehandling

    fun oppdaterUtbetalingskontroll(oppdatertKontroll: Utbetalingskontroll?, clock: Clock): Rammebehandling

    /**
     * Propagerer en endret klagebehandling gjennom aggregatet.
     *
     * Bumper bevisst ikke [sistEndret]: rammebehandlingens egen tilstand er uendret, det er klagebehandlingen som er endret.
     * Alle andre operasjoner som endrer rammebehandlingen skal oppdatere [sistEndret].
     */
    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Rammebehandling
}
