package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningMedSimulering
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.avbrytIkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimulertBeregning
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface Meldekortbehandling : AttesterbarBehandling {
    override val id: MeldekortId
    override val sakId: SakId
    override val saksnummer: Saksnummer
    override val fnr: Fnr
    override val opprettet: LocalDateTime
    override val saksbehandler: String?
    override val beslutter: String?
    override val iverksattTidspunkt: LocalDateTime?
    override val sendtTilBeslutning: LocalDateTime?
    override val attesteringer: Attesteringer

    val status: MeldekortbehandlingStatus
    val navkontor: Navkontor
    val begrunnelse: Begrunnelse?
    val ventestatus: Ventestatus
    val sistEndret: LocalDateTime
    val skalSendeVedtaksbrev: Boolean
    override val klagebehandling: Klagebehandling?

    val meldeperioder: Meldeperiodebehandlinger

    val beregning: Beregning? get() = meldeperioder.beregning

    /** Vi ønsker å kunne utbetale selvom vi ikke får simulert; så denne vil i noen tilfeller være null. */
    val simulering: Simulering?

    val periode: Periode get() = meldeperioder.totalPeriode

    val fraOgMed: LocalDate get() = periode.fraOgMed
    val tilOgMed: LocalDate get() = periode.tilOgMed

    val ingenDagerGirRett: Boolean get() = meldeperioder.ingenDagerGirRett

    val kjedeIder: NonEmptySet<MeldeperiodeKjedeId> get() = meldeperioder.kjedeIder

    val erSattPåVent: Boolean get() = ventestatus.erSattPåVent

    val erFullstendigUtfylt: Boolean get() = meldeperioder.erFullstendigUtfylt

    override val erAvsluttet
        get() = when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING, MeldekortbehandlingStatus.UNDER_BEHANDLING, MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortbehandlingStatus.UNDER_BESLUTNING -> false
            MeldekortbehandlingStatus.GODKJENT, MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET, MeldekortbehandlingStatus.AVBRUTT -> true
        }

    val erGodkjent
        get() = when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING, MeldekortbehandlingStatus.UNDER_BEHANDLING, MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortbehandlingStatus.UNDER_BESLUTNING, MeldekortbehandlingStatus.AVBRUTT -> false
            MeldekortbehandlingStatus.GODKJENT, MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> true
        }

    val beløpTotal: Int?
    val ordinærBeløp: Int?
    val barnetilleggBeløp: Int?

    val avbrutt: Avbrutt?

    override val erAvbrutt: Boolean
        get() = avbrutt != null

    val rammevedtakIder: NonEmptyList<VedtakId> get() = meldeperioder.rammevedtakIder

    val harKorrigering: Boolean get() = meldeperioder.any { it.type == MeldeperiodebehandlingType.KORRIGERING }
    val erAutomatiskBehandling: Boolean get() = this is MeldekortBehandletAutomatisk
    val erUnderkjent: Boolean get() = attesteringer.erUnderkjent()

    val erUnderAktivBehandling: Boolean get() = this.status == MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING || this.status == MeldekortbehandlingStatus.UNDER_BEHANDLING || this.status == MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING || this.status == MeldekortbehandlingStatus.UNDER_BESLUTNING || erUnderkjent

    fun erÅpen(): Boolean = !erAvsluttet

    /**
     * Oppdaterer meldeperiodene for denne behandlingen til nyeste versjoner for kjedene
     */
    fun oppdaterMeldeperioder(
        oppdaterteKjeder: MeldeperiodeKjeder,
        clock: Clock,
    ): Meldekortbehandling? {
        if (erAvsluttet) {
            return null
        }

        val oppdaterteMeldeperioder = meldeperioder.oppdaterMedNyeKjeder(oppdaterteKjeder) ?: return null

        val oppdatertTidspunkt = nå(clock)

        if (oppdaterteMeldeperioder.ingenDagerGirRett) {
            return this.avbrytIkkeRettTilTiltakspenger(
                tidspunkt = oppdatertTidspunkt,
            )
        }

        return when (this) {
            is MeldekortbehandlingManuell -> {
                this.tilUnderBehandling(
                    nyeMeldeperioder = oppdaterteMeldeperioder,
                    tidspunkt = oppdatertTidspunkt,
                )
            }

            is MeldekortUnderBehandling -> {
                this.copy(
                    meldeperioder = oppdaterteMeldeperioder,
                    simulering = null,
                    sistEndret = oppdatertTidspunkt,
                )
            }

            is MeldekortBehandletAutomatisk -> throw IllegalStateException("Automatisk meldekortbehandling skal alltid ansees som avsluttet")

            is MeldekortbehandlingAvbrutt -> throw IllegalStateException("Avbrutt meldekortbehandling skal alltid ansees som avsluttet")
        }
    }

    fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling

    fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Meldekortbehandling

    fun toSimulertBeregning(beregninger: MeldeperiodeBeregningerVedtatt): SimulertBeregning? {
        return beregning?.let {
            SimulertBeregning.create(
                beregning = it,
                eksisterendeBeregninger = beregninger,
                simulering = simulering,
            )
        }
    }

    /**
     * Validerer invarianter knyttet til klagebehandling for alle tilstander.
     * Kalles fra init-blokken i hver konkret implementasjon, på samme måte som [no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling.init].
     */
    fun initKlagebehandling() {
        if (klagebehandling == null) return

        when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> {
                require(klagebehandling!!.erKlarTilBehandling || klagebehandling!!.erFerdigstilt) {
                    "Klagebehandling knyttet til en meldekortbehandling som er KLAR_TIL_BEHANDLING må ha status KLAR_TIL_BEHANDLING/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
                }
            }

            MeldekortbehandlingStatus.UNDER_BEHANDLING -> {
                require(
                    (klagebehandling!!.erOmgjøring && (klagebehandling!!.erUnderBehandling || klagebehandling!!.erFerdigstilt)) ||
                        (klagebehandling!!.erOpprettholdt && (klagebehandling!!.omgjørEtterKA || klagebehandling!!.erFerdigstilt)),
                ) {
                    "Klagebehandling knyttet til en meldekortbehandling som er UNDER_BEHANDLING må ha status UNDER_BEHANDLING (ved omgjøring) eller OMGJØRING_ETTER_KLAGEINSTANS (ved opprettholdelse), men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
                }
            }

            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
                require(klagebehandling!!.erUnderBehandling || klagebehandling!!.omgjørEtterKA || klagebehandling!!.erFerdigstilt || klagebehandling!!.erKlarTilBehandling) {
                    "Klagebehandling knyttet til en meldekortbehandling som er KLAR_TIL_BESLUTNING må ha status UNDER_BEHANDLING/KLAR_TIL_BEHANDLING/OMGJØRING_ETTER_KLAGEINSTANS/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
                }
            }

            MeldekortbehandlingStatus.UNDER_BESLUTNING -> {
                require(klagebehandling!!.erUnderBehandling || klagebehandling!!.omgjørEtterKA || klagebehandling!!.erFerdigstilt) {
                    "Klagebehandling knyttet til en meldekortbehandling som er UNDER_BESLUTNING må ha status UNDER_BEHANDLING/OMGJØRING_ETTER_KLAGEINSTANS/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
                }
            }

            MeldekortbehandlingStatus.GODKJENT -> {
                require(klagebehandling!!.erVedtatt || klagebehandling!!.erFerdigstilt) {
                    "Klagebehandling knyttet til en meldekortbehandling som er GODKJENT må ha status VEDTATT/FERDIGSTILT, men var ${klagebehandling!!.status}. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
                }
            }

            MeldekortbehandlingStatus.AVBRUTT -> {
                // Ved avbrutt meldekortbehandling beholdes koblingen til klagebehandlingen for historikkens skyld (men ikke omvendt).
            }

            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> {
                throw IllegalStateException(
                    "Automatisk behandlet meldekortbehandling kan ikke ha en tilknyttet klagebehandling. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}",
                )
            }
        }

        // Fellessjekker for alle tilstander
        require(fnr == klagebehandling!!.fnr) {
            "Klagebehandlingens fnr må være lik meldekortbehandlingens fnr. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
        }
        require(sakId == klagebehandling!!.sakId) {
            "Klagebehandlingens sakId må være lik meldekortbehandlingens sakId. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
        }
        require(saksnummer == klagebehandling!!.saksnummer) {
            "Klagebehandlingens saksnummer må være lik meldekortbehandlingens saksnummer. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
        }

        if (!erAvbrutt) {
            require(klagebehandling!!.behandlingId.contains(this.id)) {
                "Klagebehandlingens behandlingId må inneholde meldekortbehandlingens id. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
            }
        }

        if (!erAvbrutt && !klagebehandling!!.erFerdigstilt && !klagebehandling!!.erKlarTilBehandling) {
            require(saksbehandler == klagebehandling!!.saksbehandler) {
                "Klagebehandlingens saksbehandler må være lik meldekortbehandlingens saksbehandler. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
            }
            require(klagebehandling!!.resultat is Klagebehandlingsresultat.Omgjør || klagebehandling!!.resultat is Klagebehandlingsresultat.Opprettholdt) {
                "Klagebehandlingens resultat må være Omgjør/Opprettholdt når den er knyttet til en meldekortbehandling som ikke er avbrutt. sakId: $sakId, saksnummer: $saksnummer, meldekortbehandlingId: $id, klagebehandlingId: ${klagebehandling!!.id}"
            }
        }
    }

    sealed interface Behandlet :
        Meldekortbehandling,
        BeregningMedSimulering {
        override val beregning: Beregning
        override val beløpTotal: Int get() = beregning.totalBeløp
        override val ordinærBeløp: Int get() = beregning.ordinærBeløp
        override val barnetilleggBeløp: Int get() = beregning.barnetilleggBeløp

        override fun toSimulertBeregning(beregninger: MeldeperiodeBeregningerVedtatt): SimulertBeregning {
            return super<BeregningMedSimulering>.toSimulertBeregning(beregninger)
        }

        /**
         *  Perioden for beregningen av meldekortet.
         *  Fra og med start av meldeperioden, til og med siste dag med en beregnet utbetaling
         *  Ved korrigeringer tilbake i tid kan tilOgMed strekke seg til påfølgende meldeperioder dersom disse påvirkes av beregningen
         * */
        val beregningPeriode: Periode get() = beregning.periode
    }
}
