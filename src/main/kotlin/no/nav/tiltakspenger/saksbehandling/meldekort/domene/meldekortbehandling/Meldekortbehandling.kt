package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
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
    val sistEndret: LocalDateTime
    val skalSendeVedtaksbrev: Boolean

    /** Denne styres kun av vedtakene. Dersom vi har en åpen meldekortbehandling (inkl. til beslutning) kan et nytt vedtak overstyre hele meldeperioden til [MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] */
    val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?
    val type: MeldekortbehandlingType

    val meldeperioder: Meldeperiodebehandlinger

    val beregning: Beregning? get() = meldeperioder.beregning

    /** Vi ønsker å kunne utbetale selvom vi ikke får simulert; så denne vil i noen tilfeller være null. */
    val simulering: Simulering?

    val periode: Periode get() = meldeperioder.totalPeriode

    val fraOgMed: LocalDate get() = periode.fraOgMed
    val tilOgMed: LocalDate get() = periode.tilOgMed

    /** TODO: fjernes når all funksjonalitet for å behandle flere meldeperioder samtidig er på plass */
    val meldeperiode: Meldeperiode get() = meldeperioder.single().meldeperiode
    val kjedeId: MeldeperiodeKjedeId get() = meldeperiode.kjedeId
    val brukersMeldekort: BrukersMeldekort? get() = meldeperioder.single().brukersMeldekort
    val dager: UtfyltMeldeperiode get() = meldeperioder.single().dager

    /** Merk at statusen [MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den brukes ifm stans. */
    override val erAvsluttet
        get() = when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING, MeldekortbehandlingStatus.UNDER_BEHANDLING, MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortbehandlingStatus.UNDER_BESLUTNING -> false
            MeldekortbehandlingStatus.GODKJENT, MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET, MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER, MeldekortbehandlingStatus.AVBRUTT -> true
        }

    val erGodkjentEllerIkkeRett
        get() = when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING, MeldekortbehandlingStatus.UNDER_BEHANDLING, MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortbehandlingStatus.UNDER_BESLUTNING, MeldekortbehandlingStatus.AVBRUTT -> false
            MeldekortbehandlingStatus.GODKJENT, MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET, MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> true
        }

    val erGodkjent
        get() = when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING, MeldekortbehandlingStatus.UNDER_BEHANDLING, MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING, MeldekortbehandlingStatus.UNDER_BESLUTNING, MeldekortbehandlingStatus.AVBRUTT, MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> false
            MeldekortbehandlingStatus.GODKJENT, MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET -> true
        }

    val beløpTotal: Int?
    val ordinærBeløp: Int?
    val barnetilleggBeløp: Int?

    val avbrutt: Avbrutt?

    override val erAvbrutt: Boolean
        get() = avbrutt != null

    val rammevedtakIder: NonEmptyList<VedtakId> get() = meldeperioder.rammevedtakIder

    val erKorrigering: Boolean get() = type == MeldekortbehandlingType.KORRIGERING
    val erUnderkjent: Boolean get() = attesteringer.erUnderkjent()

    /** Merk at statusen [MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den brukes ifm stans. */
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

        val ikkeRettTilTiltakspengerTidspunkt = if (oppdaterteMeldeperioder.ingenDagerGirRett) {
            nå(clock)
        } else {
            null
        }

        return when (this) {
            is MeldekortbehandlingManuell -> if (ikkeRettTilTiltakspengerTidspunkt != null) {
                this.avbrytIkkeRettTilTiltakspenger(
                    ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                )
            } else {
                this.tilUnderBehandling(
                    nyeMeldeperioder = oppdaterteMeldeperioder,
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    clock = clock,
                )
            }

            is MeldekortUnderBehandling -> if (ikkeRettTilTiltakspengerTidspunkt != null) {
                this.avbrytIkkeRettTilTiltakspenger(
                    ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                )
            } else {
                this.copy(
                    meldeperioder = oppdaterteMeldeperioder,
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    simulering = null,
                )
            }

            is MeldekortBehandletAutomatisk,
            is MeldekortbehandlingAvbrutt,
            -> null
        }
    }

    fun overta(
        saksbehandler: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Meldekortbehandling>

    fun taMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling

    fun leggTilbakeMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling
    fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling

    fun toSimulertBeregning(beregninger: MeldeperiodeBeregningerVedtatt): SimulertBeregning? {
        return beregning?.let {
            SimulertBeregning.create(
                beregning = it,
                eksisterendeBeregninger = beregninger,
                simulering = simulering,
            )
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
