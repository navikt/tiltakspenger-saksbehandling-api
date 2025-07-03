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
    override val simulering: Simulering?,
    override val status: MeldekortBehandlingStatus,
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
    ): Either<KanIkkeOppdatereMeldekort, Pair<MeldekortUnderBehandling, SimuleringMedMetadata?>> {
        validerSaksbehandlerOgTilstand(kommando.saksbehandler).onLeft {
            return it.tilKanIkkeOppdatereMeldekort().left()
        }
        val beregning = MeldekortBeregning(beregn(meldeperiode))

        val oppdatertBehandling = this.copy(
            dager = kommando.dager.tilMeldekortDager(meldeperiode),
            // Dersom saksbehandler vil tømme begrunnelsen kan hen sende en tom streng.
            begrunnelse = kommando.begrunnelse ?: this.begrunnelse,
            beregning = beregning,
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
        val (oppdatertMeldekort, simulering) = if (kommando.harOppdateringer) {
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
                simuler = simuler,
            ).getOrElse { return KanIkkeSendeMeldekortTilBeslutter.KanIkkeOppdatere(it).left() }
        } else {
            Pair(this, null)
        }

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
        krevSaksbehandlerRolle(saksbehandler)

        require(saksbehandler.navIdent == this.saksbehandler)
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

    fun avbryt(
        avbruttAv: Saksbehandler,
        begrunnelse: String,
        tidspunkt: LocalDateTime,
    ): Either<KanIkkeAvbryteMeldekortBehandling, MeldekortBehandling> {
        krevSaksbehandlerRolle(avbruttAv)
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
    }
}

fun Sak.opprettManuellMeldekortBehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    clock: Clock,
): Pair<Sak, MeldekortUnderBehandling> {
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
        simulering = null,
        dager = meldeperiode.tilMeldekortDager(),
        status = UNDER_BEHANDLING,
    ).let {
        this.leggTilMeldekortbehandling(it) to it
    }
}
