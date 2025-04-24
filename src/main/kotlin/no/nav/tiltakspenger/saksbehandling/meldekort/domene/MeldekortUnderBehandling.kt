package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BEHANDLING
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
    override val saksbehandler: String,
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
        beregning: MeldekortBeregning,
    ): Either<KanIkkeOppdatereMeldekort, MeldekortUnderBehandling> {
        val dager = validerOppdateringOgHentDager(kommando).getOrElse {
            return it.left()
        }

        return this.copy(
            dager = dager,
            begrunnelse = kommando.begrunnelse,
            beregning = beregning,
        ).right()
    }

    fun sendTilBeslutter(
        kommando: OppdaterMeldekortKommando,
        beregning: MeldekortBeregning,
        clock: Clock,
    ): Either<KanIkkeOppdatereMeldekort, MeldekortBehandletManuelt> {
        val dager = validerOppdateringOgHentDager(kommando).getOrElse {
            return it.left()
        }

        return MeldekortBehandletManuelt(
            id = this.id,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            opprettet = this.opprettet,
            beregning = beregning,
            saksbehandler = kommando.saksbehandler.navIdent,
            sendtTilBeslutning = nå(clock),
            beslutter = this.beslutter,
            status = KLAR_TIL_BESLUTNING,
            iverksattTidspunkt = null,
            navkontor = this.navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            type = type,
            begrunnelse = kommando.begrunnelse,
            attesteringer = attesteringer,
            dager = dager,
        ).right()
    }

    private fun validerOppdateringOgHentDager(kommando: OppdaterMeldekortKommando): Either<KanIkkeOppdatereMeldekort, MeldekortDager> {
        val dager = kommando.dager.tilMeldekortDager(meldeperiode.antallDagerForPeriode)
        val saksbehandler = kommando.saksbehandler

        require(dager.periode == this.meldeperiode.periode) {
            "Perioden for meldekortet må være lik meldeperioden"
        }

        if (!saksbehandler.erSaksbehandler()) {
            return KanIkkeOppdatereMeldekort.MåVæreSaksbehandler(saksbehandler.roller).left()
        }

        if (saksbehandler.navIdent != this.saksbehandler) {
            return KanIkkeOppdatereMeldekort.MåVæreSaksbehandlerForMeldekortet.left()
        }

        if (!erKlarTilUtfylling()) {
            // John har avklart med Sølvi og Taulant at vi bør ha en begrensning på at vi kan fylle ut et meldekort hvis dagens dato er innenfor meldekortperioden eller senere.
            // Dette kan endres på ved behov.
            return KanIkkeOppdatereMeldekort.MeldekortperiodenKanIkkeVæreFremITid.left()
        }

        return dager.right()
    }

    fun erKlarTilUtfylling(): Boolean {
        return !LocalDate.now().isBefore(periode.fraOgMed)
    }

    override fun underkjenn(
        begrunnelse: NonBlankString,
        beslutter: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeUnderkjenneMeldekortBehandling, MeldekortBehandling> {
        return KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeKlarTilBeslutning.left()
    }

    override fun overta(
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        return when (this.status) {
            UNDER_BEHANDLING -> {
                this.copy(
                    saksbehandler = saksbehandler.navIdent,
                ).right()
            }
            KLAR_TIL_BESLUTNING,
            GODKJENT,
            IKKE_RETT_TIL_TILTAKSPENGER,
            -> throw IllegalStateException("Kan ikke overta meldekortbehandling med status ${this.status}")
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
