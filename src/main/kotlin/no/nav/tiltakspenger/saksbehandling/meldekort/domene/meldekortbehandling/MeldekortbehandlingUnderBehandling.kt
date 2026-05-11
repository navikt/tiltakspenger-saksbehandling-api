package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Brukes for tilstandene KLAR_TIL_BEHANDLING og UNDER_BEHANDLING
 */
data class MeldekortUnderBehandling(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val navkontor: Navkontor,
    override val saksbehandler: String?,
    override val type: MeldekortbehandlingType,
    override val begrunnelse: Begrunnelse?,
    override val attesteringer: Attesteringer,
    override val sendtTilBeslutning: LocalDateTime?,
    override val simulering: Simulering?,
    override val status: MeldekortbehandlingStatus,
    override val sistEndret: LocalDateTime,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val skalSendeVedtaksbrev: Boolean,
    override val meldeperioder: Meldeperiodebehandlinger,
) : Meldekortbehandling {
    override val avbrutt: Avbrutt? = null
    override val iverksattTidspunkt = null

    override val beslutter = null

    /** Totalsummen for meldeperioden */
    override val beløpTotal = beregning?.totalBeløp
    override val ordinærBeløp = beregning?.ordinærBeløp
    override val barnetilleggBeløp = beregning?.barnetilleggBeløp

    suspend fun oppdater(
        kommando: OppdaterMeldekortbehandlingKommando,
        oppdatertePerioder: Meldeperiodebehandlinger,
        simuler: suspend (Meldekortbehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
        clock: Clock,
    ): Either<KanIkkeOppdatereMeldekortbehandling, Pair<MeldekortUnderBehandling, SimuleringMedMetadata?>> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler, oppdatertePerioder, clock).onLeft {
            return it.tilKanIkkeOppdatereMeldekort().left()
        }

        val oppdatertBehandling = this.copy(
            meldeperioder = oppdatertePerioder,
            begrunnelse = kommando.begrunnelse,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
            sistEndret = nå(clock),
        )

        // TODO jah: I første omgang kjører vi simulering som best effort. Men dersom den feiler, er det viktig at vi nuller den ut. Også kan vi senere tvinge den på, evt. kunne ha et flagg som dropper kjøre simulering.
        val simuleringMedMetadata = simuler(oppdatertBehandling).getOrElse { null }

        return Pair(
            oppdatertBehandling.oppdaterSimulering(simulering = simuleringMedMetadata?.simulering) as MeldekortUnderBehandling,
            simuleringMedMetadata,
        ).right()
    }

    fun sendTilBeslutter(
        kommando: SendMeldekortbehandlingTilBeslutterKommando,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortbehandlingTilBeslutter, MeldekortbehandlingManuell> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler, this.meldeperioder, clock).onLeft {
            return it.tilKanIkkeSendeMeldekortTilBeslutter().left()
        }

        return MeldekortbehandlingManuell(
            id = this.id,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            opprettet = this.opprettet,
            simulering = this.simulering,
            saksbehandler = this.saksbehandler!!,
            sendtTilBeslutning = nå(clock),
            beslutter = this.beslutter,
            status = MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            iverksattTidspunkt = null,
            navkontor = this.navkontor,
            type = this.type,
            begrunnelse = this.begrunnelse,
            attesteringer = this.attesteringer,
            sistEndret = nå(clock),
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            meldeperioder = this.meldeperioder,
        ).right()
    }

    sealed interface TilgangEllerTilstandsfeil {

        data object MeldekortperiodenKanIkkeVæreFremITid : TilgangEllerTilstandsfeil

        fun tilKanIkkeOppdatereMeldekort(): KanIkkeOppdatereMeldekortbehandling {
            return when (this) {
                is MeldekortperiodenKanIkkeVæreFremITid -> KanIkkeOppdatereMeldekortbehandling.MeldekortperiodenKanIkkeVæreFremITid
            }
        }

        fun tilKanIkkeSendeMeldekortTilBeslutter(): KanIkkeSendeMeldekortbehandlingTilBeslutter {
            return when (this) {
                is MeldekortperiodenKanIkkeVæreFremITid -> KanIkkeSendeMeldekortbehandlingTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid
            }
        }
    }

    private fun validerSaksbehandlerOgTilstand(
        saksbehandler: Saksbehandler,
        perioder: Meldeperiodebehandlinger,
        clock: Clock,
    ): Either<TilgangEllerTilstandsfeil, Unit> {
        require(saksbehandler.navIdent == this.saksbehandler)

        require(!perioder.ingenDagerGirRett) {
            "Meldeperiodene må ha minst en dag med rett for å kunne behandles"
        }

        if (this.status != MeldekortbehandlingStatus.UNDER_BEHANDLING) {
            throw IllegalStateException("Status må være UNDER_BEHANDLING. Kan ikke oppdatere meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler.")
        }

        if (!erKlarTilUtfylling(clock)) {
            // John har avklart med Sølvi og Taulant at vi bør ha en begrensning på at vi kan fylle ut et meldekort hvis dagens dato er innenfor meldekortperioden eller senere.
            // Dette kan endres på ved behov.
            return TilgangEllerTilstandsfeil.MeldekortperiodenKanIkkeVæreFremITid.left()
        }

        return Unit.right()
    }

    fun erKlarTilUtfylling(clock: Clock): Boolean {
        return !LocalDate.now(clock).isBefore(periode.fraOgMed)
    }

    override fun overta(
        saksbehandler: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Meldekortbehandling> {
        return when (this.status) {
            MeldekortbehandlingStatus.UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                if (this.saksbehandler == null) {
                    return KunneIkkeOvertaMeldekortbehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
                }
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    sistEndret = nå(clock),
                ).right()
            }

            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            MeldekortbehandlingStatus.UNDER_BESLUTNING,
            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.AVBRUTT,
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            -> throw IllegalStateException("Kan ikke overta meldekortbehandling med status ${this.status}")
        }
    }

    override fun taMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        return when (this.status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                require(this.saksbehandler == null) { "Meldekortbehandlingen har en eksisterende saksbehandler. For å overta meldekortbehandlingen, bruk overta() - meldekortId: ${this.id}" }
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
                    sistEndret = nå(clock),
                )
            }

            MeldekortbehandlingStatus.UNDER_BEHANDLING,
            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            MeldekortbehandlingStatus.UNDER_BESLUTNING,
            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
            MeldekortbehandlingStatus.AVBRUTT,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun leggTilbakeMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        return when (this.status) {
            MeldekortbehandlingStatus.UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                require(this.saksbehandler == saksbehandler.navIdent)
                this.copy(
                    saksbehandler = null,
                    status = MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
                    sistEndret = nå(clock),
                )
            }

            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            MeldekortbehandlingStatus.UNDER_BESLUTNING,
            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER,
            MeldekortbehandlingStatus.AVBRUTT,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling {
        require(status == MeldekortbehandlingStatus.UNDER_BEHANDLING) {
            "Kan kun oppdatere simulering på meldekortbehandling dersom status er UNDER_BEHANDLING. Status er $status, sakId: $sakId, id: $id"
        }
        return this.copy(simulering = simulering)
    }

    init {
        require(status == MeldekortbehandlingStatus.UNDER_BEHANDLING || status == MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING) {
            "Status på meldekort under behandling må være UNDER_BEHANDLING eller KLAR_TIL_BEHANDLING"
        }
    }
}

sealed interface SkalLagreEllerOppdatere {
    object Lagre : SkalLagreEllerOppdatere
    object Oppdatere : SkalLagreEllerOppdatere
}

/**
 *  Brukes både for å opprette en ny meldekortbehandling, og for å ta opp en meldekortbehandling som har blitt lagt tilbake
 *
 *  SkalLagreEllerOppdatere er en workaround for å vite hvilket database kall vi skal bruke.
 *
 * */
fun Sak.opprettManuellMeldekortbehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeOppretteMeldekortbehandling, Triple<Sak, MeldekortUnderBehandling, SkalLagreEllerOppdatere>> {
    validerOpprettManuellMeldekortbehandling(kjedeId).onLeft {
        return KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil(it).left()
    }

    val åpenMeldekortbehandling = this.meldekortbehandlinger.åpenMeldekortbehandling

    // [Sak.validerOpprettManuellMeldekortbehandling] sjekker om en evt åpen behandling kan gjenopprettes
    if (åpenMeldekortbehandling != null) {
        val oppdatertBehandling = (åpenMeldekortbehandling as MeldekortUnderBehandling).copy(
            saksbehandler = saksbehandler.navIdent,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
            sistEndret = nå(clock),
        )

        return Triple(
            this.oppdaterMeldekortbehandling(oppdatertBehandling),
            oppdatertBehandling,
            SkalLagreEllerOppdatere.Oppdatere,
        ).right()
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val behandlingerForKjede = this.meldekortbehandlinger.hentIkkeAvbrutteBehandlingerForKjede(kjedeId)
    val type =
        if (behandlingerForKjede.isEmpty()) MeldekortbehandlingType.FØRSTE_BEHANDLING else MeldekortbehandlingType.KORRIGERING

    return MeldekortUnderBehandling(
        id = MeldekortId.random(),
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(clock),
        navkontor = navkontor,
        saksbehandler = saksbehandler.navIdent,
        type = type,
        begrunnelse = null,
        attesteringer = Attesteringer.empty(),
        sendtTilBeslutning = null,
        simulering = null,
        status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        sistEndret = nå(clock),
        fritekstTilVedtaksbrev = null,
        meldeperioder = Meldeperiodebehandlinger(
            meldeperioder = nonEmptyListOf(meldeperiode.tilMeldeperiodebehandling()),
            beregning = null,
        ),
        skalSendeVedtaksbrev = true,
    ).let {
        Triple(
            this.leggTilMeldekortbehandling(it),
            it,
            SkalLagreEllerOppdatere.Lagre,
        ).right()
    }
}
