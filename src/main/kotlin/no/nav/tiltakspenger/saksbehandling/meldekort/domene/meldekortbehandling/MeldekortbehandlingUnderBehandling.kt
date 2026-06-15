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
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.oppdaterBehandlingId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.KanIkkeOppdatereMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
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
    override val begrunnelse: Begrunnelse?,
    override val attesteringer: Attesteringer,
    override val sendtTilBeslutning: LocalDateTime?,
    override val simulering: Simulering?,
    override val status: MeldekortbehandlingStatus,
    override val sistEndret: LocalDateTime,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val skalSendeVedtaksbrev: Boolean,
    override val meldeperioder: Meldeperiodebehandlinger,
    override val ventestatus: Ventestatus,
    override val klagebehandling: Klagebehandling?,
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
            begrunnelse = this.begrunnelse,
            attesteringer = this.attesteringer,
            sistEndret = nå(clock),
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            meldeperioder = this.meldeperioder,
            ventestatus = ventestatus,
            klagebehandling = klagebehandling,
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

    override fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling {
        require(status == MeldekortbehandlingStatus.UNDER_BEHANDLING) {
            "Kan kun oppdatere simulering på meldekortbehandling dersom status er UNDER_BEHANDLING. Status er $status, sakId: $sakId, id: $id"
        }
        return this.copy(simulering = simulering)
    }

    override fun oppdaterKlagebehandling(klagebehandling: Klagebehandling): Meldekortbehandling {
        require(this.klagebehandling?.id == klagebehandling.id) {
            "Kan ikke oppdatere meldekortbehandling $id med en annen klagebehandling enn den er knyttet til"
        }
        return this.copy(klagebehandling = klagebehandling)
    }

    init {
        require(status == MeldekortbehandlingStatus.UNDER_BEHANDLING || status == MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING) {
            "Status på meldekort under behandling må være UNDER_BEHANDLING eller KLAR_TIL_BEHANDLING"
        }
        initKlagebehandling()
    }
}

fun Sak.opprettManuellMeldekortbehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    klagebehandlingId: KlagebehandlingId?,
    clock: Clock,
): Either<KanIkkeOppretteMeldekortbehandling, Pair<Sak, MeldekortUnderBehandling>> {
    validerOpprettManuellMeldekortbehandling(kjedeId).onLeft {
        return KanIkkeOppretteMeldekortbehandling.ValiderOpprettFeil(it).left()
    }

    val meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(kjedeId)

    val behandlingerForKjede = this.meldekortbehandlinger.hentIkkeAvbrutteBehandlingerForKjede(kjedeId)
    val type =
        if (behandlingerForKjede.isEmpty()) MeldeperiodebehandlingType.FØRSTE_BEHANDLING else MeldeperiodebehandlingType.KORRIGERING

    val meldekortId = MeldekortId.random()
    val nå = nå(clock)

    return MeldekortUnderBehandling(
        id = meldekortId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå,
        navkontor = navkontor,
        saksbehandler = saksbehandler.navIdent,
        begrunnelse = null,
        attesteringer = Attesteringer.empty(),
        sendtTilBeslutning = null,
        simulering = null,
        status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
        sistEndret = nå,
        fritekstTilVedtaksbrev = null,
        meldeperioder = Meldeperiodebehandlinger(
            meldeperioder = nonEmptyListOf(meldeperiode.tilMeldeperiodebehandling(type)),
            beregning = null,
        ),
        skalSendeVedtaksbrev = true,
        ventestatus = Ventestatus(),
        klagebehandling = klagebehandlingId?.let {
            val klagebehandling = hentKlagebehandling(it)

            // verifiserer at vi har et meldekortvedtak vedtak som kan klages på
            this.vedtaksliste.meldekortvedtaksliste.single { it.id == klagebehandling.formkrav.vedtakDetKlagesPå }

            klagebehandling.oppdaterBehandlingId(
                behandlingId = meldekortId,
                saksbehandler = saksbehandler,
                sistEndret = nå,
            )
        },
    ).let {
        Pair(
            this.leggTilMeldekortbehandling(it),
            it,
        ).right()
    }
}
