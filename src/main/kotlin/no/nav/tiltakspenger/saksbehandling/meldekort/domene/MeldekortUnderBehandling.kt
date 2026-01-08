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
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Gjelder tilstandene UNDER_BEHANDLING og KLAR_TIL_BESLUTNING.
 */
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
    override val begrunnelse: Begrunnelse?,
    override val attesteringer: Attesteringer,
    override val sendtTilBeslutning: LocalDateTime?,
    override val dager: MeldekortDager,
    override val beregning: Beregning?,
    override val simulering: Simulering?,
    override val status: MeldekortBehandlingStatus,
    override val sistEndret: LocalDateTime,
    override val behandlingSendtTilDatadeling: LocalDateTime?,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
) : MeldekortBehandling {
    override val avbrutt: Avbrutt? = null
    override val iverksattTidspunkt = null

    override val beslutter = null

    /** Totalsummen for meldeperioden */
    override val beløpTotal = beregning?.totalBeløp
    override val ordinærBeløp = beregning?.ordinærBeløp
    override val barnetilleggBeløp = beregning?.barnetilleggBeløp

    suspend fun oppdater(
        kommando: OppdaterMeldekortKommando,
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
        simuler: suspend (MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
        clock: Clock,
    ): Either<KanIkkeOppdatereMeldekort, Pair<MeldekortUnderBehandling, SimuleringMedMetadata?>> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler).onLeft {
            return it.tilKanIkkeOppdatereMeldekort().left()
        }
        val beregning = Beregning(beregn(meldeperiode))

        val oppdatertBehandling = this.copy(
            dager = kommando.dager.tilMeldekortDager(meldeperiode),
            begrunnelse = kommando.begrunnelse,
            beregning = beregning,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            sistEndret = nå(clock),
        )
        // TODO jah: I første omgang kjører vi simulering som best effort. Men dersom den feiler, er det viktig at vi nuller den ut. Også kan vi senere tvinge den på, evt. kunne ha et flagg som dropper kjøre simulering.
        val simuleringMedMetadata = simuler(oppdatertBehandling).getOrElse { null }
        return Pair(
            oppdatertBehandling.copy(simulering = simuleringMedMetadata?.simulering),
            simuleringMedMetadata,
        ).right()
    }

    suspend fun sendTilBeslutter(
        kommando: SendMeldekortTilBeslutterKommando,
        beregn: (meldeperiode: Meldeperiode) -> NonEmptyList<MeldeperiodeBeregning>,
        simuler: suspend (MeldekortBehandling) -> Either<KunneIkkeSimulere, SimuleringMedMetadata>,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortTilBeslutter, Pair<MeldekortBehandletManuelt, SimuleringMedMetadata?>> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler).onLeft {
            return it.tilKanIkkeSendeMeldekortTilBeslutter().left()
        }

        val (oppdatertMeldekort, simulering) = oppdater(
            kommando = OppdaterMeldekortKommando(
                sakId = kommando.sakId,
                meldekortId = kommando.meldekortId,
                saksbehandler = kommando.saksbehandler,
                dager = kommando.dager!!,
                begrunnelse = kommando.begrunnelse,
                correlationId = kommando.correlationId,
                fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            ),
            beregn = beregn,
            simuler = simuler,
            clock = clock,
        ).getOrElse { return KanIkkeSendeMeldekortTilBeslutter.KanIkkeOppdatere(it).left() }

        return (
            MeldekortBehandletManuelt(
                id = oppdatertMeldekort.id,
                sakId = oppdatertMeldekort.sakId,
                saksnummer = oppdatertMeldekort.saksnummer,
                fnr = oppdatertMeldekort.fnr,
                opprettet = oppdatertMeldekort.opprettet,
                beregning = oppdatertMeldekort.beregning!!,
                simulering = oppdatertMeldekort.simulering,
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
                sistEndret = nå(clock),
                behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
                fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            ) to simulering
            ).right()
    }

    sealed interface TilgangEllerTilstandsfeil {

        data object MeldekortperiodenKanIkkeVæreFremITid : TilgangEllerTilstandsfeil

        fun tilKanIkkeOppdatereMeldekort(): KanIkkeOppdatereMeldekort {
            return when (this) {
                is MeldekortperiodenKanIkkeVæreFremITid -> KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid
            }
        }

        fun tilKanIkkeSendeMeldekortTilBeslutter(): KanIkkeSendeMeldekortTilBeslutter {
            return when (this) {
                is MeldekortperiodenKanIkkeVæreFremITid -> KanIkkeSendeMeldekortTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid
            }
        }
    }

    private fun validerSaksbehandlerOgTilstand(saksbehandler: Saksbehandler): Either<TilgangEllerTilstandsfeil, Unit> {
        require(saksbehandler.navIdent == this.saksbehandler)

        require(!this.meldeperiode.ingenDagerGirRett) {
            "Meldeperioden må ha minst en dag med rett for å kunne behandles - meldeperiode: ${this.meldeperiode.id}"
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
                krevSaksbehandlerRolle(saksbehandler)
                if (this.saksbehandler == null) {
                    return KunneIkkeOvertaMeldekortBehandling.BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta.left()
                }
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    sistEndret = LocalDateTime.now(),
                ).right()
            }

            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            GODKJENT,
            IKKE_RETT_TIL_TILTAKSPENGER,
            AUTOMATISK_BEHANDLET,
            AVBRUTT,
            KLAR_TIL_BEHANDLING,
            -> throw IllegalStateException("Kan ikke overta meldekortbehandling med status ${this.status}")
        }
    }

    override fun taMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        return when (this.status) {
            KLAR_TIL_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                require(this.saksbehandler == null) { "Meldekortbehandlingen har en eksisterende saksbehandler. For å overta meldekortbehandlingen, bruk overta() - meldekortId: ${this.id}" }
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                    status = UNDER_BEHANDLING,
                    sistEndret = LocalDateTime.now(),
                )
            }

            UNDER_BEHANDLING,
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            GODKJENT,
            AUTOMATISK_BEHANDLET,
            IKKE_RETT_TIL_TILTAKSPENGER,
            AVBRUTT,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun leggTilbakeMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        return when (this.status) {
            UNDER_BEHANDLING -> {
                krevSaksbehandlerRolle(saksbehandler)
                require(this.saksbehandler == saksbehandler.navIdent)
                this.copy(
                    saksbehandler = null,
                    status = KLAR_TIL_BEHANDLING,
                    sistEndret = LocalDateTime.now(),
                )
            }

            KLAR_TIL_BEHANDLING,
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            GODKJENT,
            AUTOMATISK_BEHANDLET,
            IKKE_RETT_TIL_TILTAKSPENGER,
            AVBRUTT,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun oppdaterSimulering(simulering: Simulering?): MeldekortBehandling {
        require(status == UNDER_BEHANDLING) {
            "Kan kun oppdatere simulering på meldekortbehandling dersom status er UNDER_BEHANDLING. Status er $status, sakId: $sakId, id: $id"
        }
        return this.copy(simulering = simulering)
    }

    fun avbryt(
        avbruttAv: Saksbehandler,
        begrunnelse: String,
        tidspunkt: LocalDateTime,
    ): Either<KanIkkeAvbryteMeldekortBehandling, MeldekortBehandling> {
        require(this.status == UNDER_BEHANDLING) {
            return KanIkkeAvbryteMeldekortBehandling.MåVæreUnderBehandling.left()
        }
        require(this.saksbehandler == avbruttAv.navIdent) {
            return KanIkkeAvbryteMeldekortBehandling.MåVæreSaksbehandlerForMeldekortet.left()
        }

        return AvbruttMeldekortBehandling(
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
            behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
        ).right()
    }

    fun avbrytIkkeRettTilTiltakspenger(
        ikkeRettTilTiltakspengerTidspunkt: LocalDateTime,
    ): AvbruttMeldekortBehandling {
        return AvbruttMeldekortBehandling(
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
                begrunnelse = "Ikke rett til tiltakspenger",
            ),
            sistEndret = ikkeRettTilTiltakspengerTidspunkt,
            behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
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

        require(status == UNDER_BEHANDLING || status == KLAR_TIL_BEHANDLING) {
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
fun Sak.opprettManuellMeldekortBehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    clock: Clock,
): Either<KanIkkeOppretteMeldekortbehandling, Triple<Sak, MeldekortUnderBehandling, SkalLagreEllerOppdatere>> {
    validerOpprettManuellMeldekortbehandling(kjedeId).onLeft {
        return KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil(it).left()
    }

    val åpenMeldekortBehandling = this.meldekortbehandlinger.åpenMeldekortBehandling

    /** [Sak.validerOpprettManuellMeldekortbehandling] sjekker om en evt åpen behandling kan gjenopprettes */
    if (åpenMeldekortBehandling != null) {
        val oppdatertBehandling = (åpenMeldekortBehandling as MeldekortUnderBehandling).copy(
            saksbehandler = saksbehandler.navIdent,
            status = UNDER_BEHANDLING,
            sistEndret = nå(clock),
        )

        return Triple(
            this.oppdaterMeldekortbehandling(oppdatertBehandling),
            oppdatertBehandling,
            SkalLagreEllerOppdatere.Oppdatere,
        ).right()
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val behandlingerForKjede = this.meldekortbehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)
    val type =
        if (behandlingerForKjede.isEmpty()) MeldekortBehandlingType.FØRSTE_BEHANDLING else MeldekortBehandlingType.KORRIGERING

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
        status = UNDER_BEHANDLING,
        sistEndret = nå(clock),
        behandlingSendtTilDatadeling = null,
        fritekstTilVedtaksbrev = null,
    ).let {
        Triple(
            this.leggTilMeldekortbehandling(it),
            it,
            SkalLagreEllerOppdatere.Lagre,
        ).right()
    }
}
