package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.avbryt.KanIkkeAvbryteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.KanIkkeSendeMeldekortbehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilBeslutter.SendMeldekortbehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDager
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
    override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
    override val saksbehandler: String?,
    override val type: MeldekortbehandlingType,
    override val begrunnelse: Begrunnelse?,
    override val attesteringer: Attesteringer,
    override val sendtTilBeslutning: LocalDateTime?,
    override val beregning: Beregning?,
    override val simulering: Simulering?,
    override val status: MeldekortbehandlingStatus,
    override val sistEndret: LocalDateTime,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val meldeperioder: BehandledeMeldeperioder,
    override val skalSendeVedtaksbrev: Boolean,
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
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
        simuler: suspend (Meldekortbehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
        clock: Clock,
    ): Either<KanIkkeOppdatereMeldekortbehandling, Pair<MeldekortUnderBehandling, SimuleringMedMetadata?>> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler, clock).onLeft {
            return it.tilKanIkkeOppdatereMeldekort().left()
        }
        val tidspunkt = nå(clock)

        val beregning = Beregning(beregn(meldeperiode), tidspunkt)

        val oppdatertBehandling = this.copy(
            dager = kommando.dager.tilMeldekortDager(meldeperiode),
            begrunnelse = kommando.begrunnelse,
            beregning = beregning,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
            sistEndret = tidspunkt,
        )

        // TODO jah: I første omgang kjører vi simulering som best effort. Men dersom den feiler, er det viktig at vi nuller den ut. Også kan vi senere tvinge den på, evt. kunne ha et flagg som dropper kjøre simulering.
        val simuleringMedMetadata = simuler(oppdatertBehandling).getOrElse { null }
        return Pair(
            oppdatertBehandling.copy(simulering = simuleringMedMetadata?.simulering),
            simuleringMedMetadata,
        ).right()
    }

    fun sendTilBeslutter(
        kommando: SendMeldekortbehandlingTilBeslutterKommando,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortbehandlingTilBeslutter, MeldekortbehandlingManuell> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler, clock).onLeft {
            return it.tilKanIkkeSendeMeldekortTilBeslutter().left()
        }

        return MeldekortbehandlingManuell(
            id = this.id,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            opprettet = this.opprettet,
            beregning = this.beregning!!,
            simulering = this.simulering,
            saksbehandler = this.saksbehandler!!,
            sendtTilBeslutning = nå(clock),
            beslutter = this.beslutter,
            status = MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            iverksattTidspunkt = null,
            navkontor = this.navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            brukersMeldekort = this.brukersMeldekort,
            meldeperiode = this.meldeperiode,
            type = this.type,
            begrunnelse = this.begrunnelse,
            attesteringer = this.attesteringer,
            dager = this.dager,
            sistEndret = nå(clock),
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
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

    private fun validerSaksbehandlerOgTilstand(saksbehandler: Saksbehandler, clock: Clock): Either<TilgangEllerTilstandsfeil, Unit> {
        require(saksbehandler.navIdent == this.saksbehandler)

        require(!this.meldeperiode.ingenDagerGirRett) {
            "Meldeperioden må ha minst en dag med rett for å kunne behandles - meldeperiode: ${this.meldeperiode.id}"
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

    fun avbryt(
        avbruttAv: Saksbehandler,
        begrunnelse: NonBlankString,
        tidspunkt: LocalDateTime,
    ): Either<KanIkkeAvbryteMeldekortbehandling, Meldekortbehandling> {
        require(this.status == MeldekortbehandlingStatus.UNDER_BEHANDLING) {
            return KanIkkeAvbryteMeldekortbehandling.MåVæreUnderBehandling.left()
        }
        require(this.saksbehandler == avbruttAv.navIdent) {
            return KanIkkeAvbryteMeldekortbehandling.MåVæreSaksbehandlerForMeldekortet.left()
        }

        return MeldekortbehandlingAvbrutt(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            beregning = beregning,
            simulering = simulering,
            saksbehandler = avbruttAv.navIdent,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            type = type,
            begrunnelse = this.begrunnelse,
            attesteringer = attesteringer,
            dager = dager,
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
            sistEndret = tidspunkt,
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        ).right()
    }

    fun avbrytIkkeRettTilTiltakspenger(
        ikkeRettTilTiltakspengerTidspunkt: LocalDateTime,
    ): MeldekortbehandlingAvbrutt {
        return MeldekortbehandlingAvbrutt(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            beregning = null,
            simulering = null,
            saksbehandler = saksbehandler,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            type = type,
            begrunnelse = begrunnelse,
            attesteringer = attesteringer,
            dager = dager,
            avbrutt = Avbrutt(
                tidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID,
                begrunnelse = "Ikke rett til tiltakspenger".toNonBlankString(),
            ),
            sistEndret = ikkeRettTilTiltakspengerTidspunkt,
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        )
    }

    init {
        require(dager.periode == this.meldeperiode.periode) {
            "Perioden for meldekortet må være lik meldeperioden"
        }
        require(dager.meldeperiode == meldeperiode) {
            "Meldekortdager.meldeperiode må være lik meldeperioden"
        }
        require(ikkeRettTilTiltakspengerTidspunkt == null) {
            "Behandlinger der det ikke er rett til tiltakspenger skal ikke være under behandling"
        }

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

    val behandlingerForKjede = this.meldekortbehandlinger.hentMeldekortbehandlingerForKjede(kjedeId)
    val type =
        if (behandlingerForKjede.isEmpty()) MeldekortbehandlingType.FØRSTE_BEHANDLING else MeldekortbehandlingType.KORRIGERING

    return MeldekortUnderBehandling(
        id = MeldekortId.random(),
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
        simulering = null,
        dager = meldeperiode.tilMeldekortDager(),
        status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        sistEndret = nå(clock),
        fritekstTilVedtaksbrev = null,
        skalSendeVedtaksbrev = true,
    ).let {
        Triple(
            this.leggTilMeldekortbehandling(it),
            it,
            SkalLagreEllerOppdatere.Lagre,
        ).right()
    }
}
