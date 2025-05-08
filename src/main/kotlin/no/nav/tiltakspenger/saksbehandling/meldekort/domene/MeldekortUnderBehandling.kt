package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortUnderBehandling(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val navkontor: Navkontor,
    override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
    override val brukersMeldekort: BrukersMeldekort?,
    override val meldeperiode: Meldeperiode,
    override val saksbehandler: String?,
    override val type: MeldekortBehandlingType,
    override val begrunnelse: MeldekortBehandlingBegrunnelse?,
    override val attesteringer: Attesteringer,
    override val sendtTilBeslutning: LocalDateTime?,
    override val dager: MeldekortDager,
    override val beregning: MeldekortBeregning?,
) : MeldekortBehandling {
    override val iverksattTidspunkt = null

    override val status =
        if (ikkeRettTilTiltakspengerTidspunkt == null) UNDER_BEHANDLING else IKKE_RETT_TIL_TILTAKSPENGER

    override val beslutter = null

    /** Totalsummen for meldeperioden */
    override val beløpTotal = beregning?.beregnTotaltBeløp()
    override val ordinærBeløp = beregning?.beregnTotalOrdinærBeløp()
    override val barnetilleggBeløp = beregning?.beregnTotalBarnetillegg()

    fun oppdater(
        kommando: OppdaterMeldekortKommando,
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
    ): Either<KanIkkeOppdatereMeldekort, MeldekortUnderBehandling> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler).onLeft { return it.tilKanIkkeOppdatereMeldekort().left() }
        val beregning = MeldekortBeregning(beregn(meldeperiode))
        return this.copy(
            dager = dager,
            // Dersom saksbehandler vil tømme begrunnelsen kan hen sende en tom streng.
            begrunnelse = kommando.begrunnelse ?: this.begrunnelse,
            beregning = beregning,
        ).right()
    }

    fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutterKommando,
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortTilBeslutter, MeldekortBehandletManuelt> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler).onLeft { return it.tilKanIkkeSendeMeldekortTilBeslutter().left() }
        val oppdatertMeldekort: MeldekortUnderBehandling = if (kommando.harOppdateringer) {
            oppdater(
                kommando = OppdaterMeldekortKommando(
                    sakId = kommando.sakId,
                    meldekortId = kommando.meldekortId,
                    saksbehandler = kommando.saksbehandler,
                    dager = kommando.dager!!,
                    begrunnelse = kommando.begrunnelse,
                    correlationId = kommando.correlationId,
                ),
                beregn = beregn,
            ).getOrElse { return KanIkkeSendeMeldekortTilBeslutter.KanIkkeOppdatere(it).left() }
        } else {
            this
        }

        return MeldekortBehandletManuelt(
            id = oppdatertMeldekort.id,
            sakId = oppdatertMeldekort.sakId,
            saksnummer = oppdatertMeldekort.saksnummer,
            fnr = oppdatertMeldekort.fnr,
            opprettet = oppdatertMeldekort.opprettet,
            beregning = oppdatertMeldekort.beregning!!,
            saksbehandler = oppdatertMeldekort.saksbehandler!!,
            sendtTilBeslutning = nå(clock),
            beslutter = oppdatertMeldekort.beslutter,
            status = KLAR_TIL_BESLUTNING,
            iverksattTidspunkt = null,
            navkontor = oppdatertMeldekort.navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            brukersMeldekort = oppdatertMeldekort.brukersMeldekort,
            meldeperiode = oppdatertMeldekort.meldeperiode,
            type = oppdatertMeldekort.type,
            begrunnelse = oppdatertMeldekort.begrunnelse,
            attesteringer = oppdatertMeldekort.attesteringer,
            dager = oppdatertMeldekort.dager,
        ).right()
    }

    sealed interface TilgangEllerTilstandsfeil {
        data class MåVæreSaksbehandler(val roller: Saksbehandlerroller) : TilgangEllerTilstandsfeil
        data object MåVæreSaksbehandlerForMeldekortet : TilgangEllerTilstandsfeil
        data object MeldekortperiodenKanIkkeVæreFremITid : TilgangEllerTilstandsfeil

        fun tilKanIkkeOppdatereMeldekort(): KanIkkeOppdatereMeldekort {
            return when (this) {
                is MåVæreSaksbehandler -> KanIkkeOppdatereMeldekort.MåVæreSaksbehandler(roller)
                is MåVæreSaksbehandlerForMeldekortet -> KanIkkeOppdatereMeldekort.MåVæreSaksbehandlerForMeldekortet
                is MeldekortperiodenKanIkkeVæreFremITid -> KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid
            }
        }

        fun tilKanIkkeSendeMeldekortTilBeslutter(): KanIkkeSendeMeldekortTilBeslutter {
            return when (this) {
                is MåVæreSaksbehandler -> KanIkkeSendeMeldekortTilBeslutter.MåVæreSaksbehandler(roller)
                is MåVæreSaksbehandlerForMeldekortet -> KanIkkeSendeMeldekortTilBeslutter.MåVæreSaksbehandlerForMeldekortet
                is MeldekortperiodenKanIkkeVæreFremITid -> KanIkkeSendeMeldekortTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid
            }
        }
    }

    private fun validerSaksbehandlerOgTilstand(saksbehandler: Saksbehandler): Either<TilgangEllerTilstandsfeil, Unit> {
        if (!saksbehandler.erSaksbehandler()) {
            return TilgangEllerTilstandsfeil.MåVæreSaksbehandler(saksbehandler.roller).left()
        }
        if (saksbehandler.navIdent != this.saksbehandler) {
            return TilgangEllerTilstandsfeil.MåVæreSaksbehandlerForMeldekortet.left()
        }
        if (this.status != UNDER_BEHANDLING) {
            throw IllegalStateException("Status må være UNDER_BEHANDLING. Kan ikke oppdatere meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler.")
        }
        if (!erKlarTilUtfylling()) {
            // John har avklart med Sølvi og Taulant at vi bør ha en begrensning på at vi kan fylle ut et meldekort hvis dagens dato er innenfor meldekortperioden eller senere.
            // Dette kan endres på ved behov.
            return TilgangEllerTilstandsfeil.MeldekortperiodenKanIkkeVæreFremITid.left()
        }

        return Unit.right()
    }

    fun erKlarTilUtfylling(): Boolean {
        return !LocalDate.now().isBefore(periode.fraOgMed)
    }

    override fun overta(
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        return when (this.status) {
            UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                if (this.saksbehandler == null) {
                    return KunneIkkeOvertaMeldekortBehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
                }
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                ).right()
            }

            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            GODKJENT,
            IKKE_RETT_TIL_TILTAKSPENGER,
            AUTOMATISK_BEHANDLET,
            -> throw IllegalStateException("Kan ikke overta meldekortbehandling med status ${this.status}")
        }
    }

    override fun taMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        return when (this.status) {
            UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                require(this.saksbehandler == null) { "Meldekortbehandlingen har en eksisterende saksbehandler. For å overta meldekortbehandlingen, bruk overta() - meldekortId: ${this.id}" }
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                )
            }

            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            GODKJENT,
            AUTOMATISK_BEHANDLET,
            IKKE_RETT_TIL_TILTAKSPENGER,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun leggTilbakeMeldekortBehandling(saksbehandler: Saksbehandler): Either<KanIkkeLeggeTilbakeMeldekortBehandling, MeldekortBehandling> {
        return when (this.status) {
            UNDER_BEHANDLING -> {
                check(saksbehandler.erSaksbehandler()) {
                    "Saksbehandler må ha rolle saksbehandler. Utøvende saksbehandler: $saksbehandler"
                }
                require(this.saksbehandler == saksbehandler.navIdent) {
                    return KanIkkeLeggeTilbakeMeldekortBehandling.MåVæreSaksbehandlerEllerBeslutterForBehandlingen.left()
                }
                this.copy(
                    saksbehandler = null,
                ).right()
            }

            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            GODKJENT,
            AUTOMATISK_BEHANDLET,
            IKKE_RETT_TIL_TILTAKSPENGER,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    init {
        if (status == IKKE_RETT_TIL_TILTAKSPENGER) {
            require(dager.all { it.status == MeldekortDagStatus.SPERRET })
        }
    }
}

/**
 * TODO post-mvp jah: Ved revurderinger av rammevedtaket, så må vi basere oss på både forrige meldekort og revurderingsvedtaket. Dette løser vi å flytte mer logikk til Sak.kt.
 * TODO post-mvp jah: Når vi implementerer delvis innvilgelse vil hele meldekortperioder kunne bli SPERRET.
 */
fun Sak.opprettManuellMeldekortBehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    clock: Clock,
): MeldekortUnderBehandling {
    validerOpprettMeldekortBehandling(kjedeId)

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val behandlingerForKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)
    val type =
        if (behandlingerForKjede.isEmpty()) MeldekortBehandlingType.FØRSTE_BEHANDLING else MeldekortBehandlingType.KORRIGERING

    val meldekortId = MeldekortId.random()

    return MeldekortUnderBehandling(
        id = meldekortId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(clock),
        navkontor = navkontor,
        ikkeRettTilTiltakspengerTidspunkt = null,
        brukersMeldekort = null,
        meldeperiode = meldeperiode,
        saksbehandler = saksbehandler.navIdent,
        type = type,
        begrunnelse = null,
        attesteringer = Attesteringer.empty(),
        sendtTilBeslutning = null,
        beregning = null,
        dager = meldeperiode.tilMeldekortDager(),
    )
}
