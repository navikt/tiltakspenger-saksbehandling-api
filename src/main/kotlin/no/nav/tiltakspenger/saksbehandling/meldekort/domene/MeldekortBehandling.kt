package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.beregning.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * TODO: splitt denne i separate hierarkier for 1. alle states av manuell behandling og 2. automatisk behandling
 * */

sealed interface MeldekortBehandling {
    val id: MeldekortId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val opprettet: LocalDateTime
    val dager: MeldekortDager
    val beregning: MeldekortBeregning?

    /** Vi ønsker å kunne utbetale selvom vi ikke får simulert; så denne vil i noen tilfeller være null. */
    val simulering: Simulering?
    val meldeperiode: Meldeperiode
    val type: MeldekortBehandlingType

    /** Pdd har kun automatiske behandlinger tilknyttet et brukers meldekort */
    val brukersMeldekort: BrukersMeldekort?
    val kjedeId: MeldeperiodeKjedeId get() = meldeperiode.kjedeId
    val periode: Periode get() = meldeperiode.periode
    val fraOgMed: LocalDate get() = periode.fraOgMed
    val tilOgMed: LocalDate get() = periode.tilOgMed

    val saksbehandler: String?
    val beslutter: String?
    val status: MeldekortBehandlingStatus
    val navkontor: Navkontor
    val iverksattTidspunkt: LocalDateTime?
    val sendtTilBeslutning: LocalDateTime?
    val begrunnelse: MeldekortBehandlingBegrunnelse?

    val attesteringer: Attesteringer

    /** Denne styres kun av vedtakene. Dersom vi har en åpen meldekortbehandling (inkl. til beslutning) kan et nytt vedtak overstyre hele meldeperioden til [MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] */
    val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den brukes ifm stans. */
    val erAvsluttet
        get() = when (status) {
            KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> false
            GODKJENT, AUTOMATISK_BEHANDLET, IKKE_RETT_TIL_TILTAKSPENGER, AVBRUTT -> true
        }

    val beløpTotal: Int?
    val ordinærBeløp: Int?
    val barnetilleggBeløp: Int?

    val avbrutt: Avbrutt?

    val rammevedtak: List<VedtakId>? get() = meldeperiode.rammevedtak?.verdier?.distinct()

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den brukes ifm stans. */
    fun erÅpen(): Boolean = !erAvsluttet

    /**
     * Oppdaterer meldeperioden til [meldeperiode] dersom den har samme kjede id, den er nyere enn den eksisterende og dette ikke er avsluttet meldekortbehandling.
     * @param tiltakstypePerioder kan være tom eller inneholde hull.
     */
    fun oppdaterMeldeperiode(
        meldeperiode: Meldeperiode,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett>,
        clock: Clock,
    ): MeldekortBehandling? {
        require(meldeperiode.kjedeId == kjedeId) {
            "MeldekortBehandling: Kan ikke oppdatere meldeperiode med annen kjede id. ${meldeperiode.kjedeId} != $kjedeId"
        }
        if (erAvsluttet) return null
        if (meldeperiode.versjon <= this.meldeperiode.versjon) return null

        val ikkeRettTilTiltakspengerTidspunkt = if (meldeperiode.ingenDagerGirRett) nå(clock) else null
        return when (this) {
            is MeldekortBehandletManuelt -> if (ikkeRettTilTiltakspengerTidspunkt != null) {
                this.avbrytIkkeRettTilTiltakspenger(
                    ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                )
            } else {
                this.tilUnderBehandling(
                    nyMeldeperiode = meldeperiode,
                    ikkeRettTilTiltakspengerTidspunkt = null,
                )
            }

            is MeldekortUnderBehandling -> if (ikkeRettTilTiltakspengerTidspunkt != null) {
                this.avbrytIkkeRettTilTiltakspenger(
                    ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                )
            } else {
                this.copy(
                    meldeperiode = meldeperiode,
                    dager = meldeperiode.tilMeldekortDager(),
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    beregning = null,
                    simulering = null,
                )
            }

            is MeldekortBehandletAutomatisk,
            is AvbruttMeldekortBehandling,
            -> null
        }
    }

    fun overta(saksbehandler: Saksbehandler): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling>

    fun taMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling

    fun leggTilbakeMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling

    sealed interface Behandlet : MeldekortBehandling {
        override val beregning: MeldekortBeregning
        override val beløpTotal: Int get() = beregning.totalBeløp
        override val ordinærBeløp: Int get() = beregning.ordinærBeløp
        override val barnetilleggBeløp: Int get() = beregning.barnetilleggBeløp

        /**
         *  Perioden for beregningen av meldekortet.
         *  Fra og med start av meldeperioden, til og med siste dag med en beregnet utbetaling
         *  Ved korrigeringer tilbake i tid kan tilOgMed strekke seg til påfølgende meldeperioder dersom disse påvirkes av beregningen
         * */
        val beregningPeriode: Periode get() = beregning.periode
    }
}

fun Sak.validerOpprettMeldekortBehandling(kjedeId: MeldeperiodeKjedeId) {
    val meldeperiodekjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)!!
    val meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()

    val åpenBehandling = this.meldekortBehandlinger.åpenMeldekortBehandling

    if (åpenBehandling != null) {
        throw IllegalStateException(
            "Kan ikke opprette ny meldekortbehandling dersom en behandling er åpen på saken - ${åpenBehandling.id} er åpen på ${this.id}",
        )
    }

    if (this.meldekortBehandlinger.isEmpty() &&
        meldeperiode != this.meldeperiodeKjeder.first()
            .hentSisteMeldeperiode()
    ) {
        throw IllegalStateException(
            "Dette er første meldekortbehandling på saken og må da behandle den første meldeperiode kjeden. sakId: ${this.id}, meldeperiodekjedeId: ${meldeperiodekjede.kjedeId}",
        )
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)
        ?.also { foregåendeMeldeperiodekjede ->
            this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId)
                .also { behandlinger ->
                    if (behandlinger.none { it.status == GODKJENT || it.status == AUTOMATISK_BEHANDLET }) {
                        throw IllegalStateException("Kan ikke opprette ny meldekortbehandling før forrige kjede er godkjent")
                    }
                }
        }

    if (meldeperiode.ingenDagerGirRett) {
        throw IllegalStateException("Kan ikke starte behandling på meldeperiode uten dager som gir rett til tiltakspenger")
    }
}
