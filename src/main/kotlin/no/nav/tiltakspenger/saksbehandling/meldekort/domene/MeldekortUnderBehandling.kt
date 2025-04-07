package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
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
        if (ikkeRettTilTiltakspengerTidspunkt == null) IKKE_BEHANDLET else IKKE_RETT_TIL_TILTAKSPENGER

    override val beslutter = null

    /** Totalsummen for meldeperioden */
    override val beløpTotal = beregning?.beregnTotaltBeløp()
    override val ordinærBeløp = beregning?.beregnTotalOrdinærBeløp()
    override val barnetilleggBeløp = beregning?.beregnTotalBarnetillegg()

    fun sendTilBeslutter(
        dager: MeldekortDager,
        beregning: MeldekortBeregning,
        begrunnelse: MeldekortBehandlingBegrunnelse?,
        saksbehandler: Saksbehandler,
        clock: Clock,
    ): Either<KanIkkeSendeMeldekortTilBeslutning, MeldekortBehandlet> {
        require(dager.periode == this.meldeperiode.periode) {
            "Perioden for meldekortet må være lik meldeperioden"
        }
        if (!saksbehandler.erSaksbehandler()) {
            return KanIkkeSendeMeldekortTilBeslutning.MåVæreSaksbehandler(saksbehandler.roller).left()
        }
        if (!erKlarTilUtfylling()) {
            // John har avklart med Sølvi og Taulant at vi bør ha en begrensning på at vi kan fylle ut et meldekort hvis dagens dato er innenfor meldekortperioden eller senere.
            // Dette kan endres på ved behov.
            return KanIkkeSendeMeldekortTilBeslutning.MeldekortperiodenKanIkkeVæreFremITid.left()
        }
        return MeldekortBehandlet(
            id = this.id,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            opprettet = this.opprettet,
            beregning = beregning,
            saksbehandler = saksbehandler.navIdent,
            sendtTilBeslutning = nå(clock),
            beslutter = this.beslutter,
            status = KLAR_TIL_BESLUTNING,
            iverksattTidspunkt = null,
            navkontor = this.navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            type = type,
            begrunnelse = begrunnelse,
            attesteringer = attesteringer,
            dager = dager,
        ).right()
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
fun Sak.opprettMeldekortBehandling(
    kjedeId: MeldeperiodeKjedeId,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
    clock: Clock,
): MeldekortUnderBehandling {
    if (this.meldekortBehandlinger.finnesÅpenMeldekortBehandling) {
        throw IllegalStateException("Kan ikke opprette ny meldekortbehandling når det finnes en åpen behandling på saken (sak $id - kjedeId $kjedeId)")
    }

    val meldeperiodekjede: MeldeperiodeKjede = this.meldeperiodeKjeder.hentMeldeperiodekjedeForKjedeId(kjedeId)
        ?: throw IllegalStateException("Kan ikke opprette meldekortbehandling for kjedeId $kjedeId som ikke finnes")
    val meldeperiode: Meldeperiode = meldeperiodekjede.hentSisteMeldeperiode()
    val behandlingerKnyttetTilKjede = this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(kjedeId)

    if (this.meldekortBehandlinger.isEmpty()) {
        require(meldeperiode == this.meldeperiodeKjeder.first().hentSisteMeldeperiode()) {
            "Dette er første meldekortbehandling på saken og må da behandle den første meldeperiode kjeden. sakId: ${this.id}, meldeperiodekjedeId: ${meldeperiodekjede.kjedeId}"
        }
    }

    this.meldeperiodeKjeder.hentForegåendeMeldeperiodekjede(kjedeId)
        ?.also { foregåendeMeldeperiodekjede ->
            this.meldekortBehandlinger.hentMeldekortBehandlingerForKjede(foregåendeMeldeperiodekjede.kjedeId).also {
                if (it.none { it.status == MeldekortBehandlingStatus.GODKJENT }) {
                    throw IllegalStateException("Kan ikke opprette ny meldekortbehandling før forrige kjede er godkjent")
                }
            }
        }

    if (meldeperiode.ingenDagerGirRett) {
        throw IllegalStateException("Kan ikke starte behandling på meldeperiode uten dager som gir rett til tiltakspenger")
    }

    // TODO abn: må støtte flere brukers meldekort på samme kjede før vi åpner for korrigering fra bruker
    val brukersMeldekort = this.brukersMeldekort.find { it.kjedeId == kjedeId }

    val type =
        if (behandlingerKnyttetTilKjede.isEmpty()) MeldekortBehandlingType.FØRSTE_BEHANDLING else MeldekortBehandlingType.KORRIGERING

    val meldekortId = MeldekortId.random()

    return MeldekortUnderBehandling(
        id = meldekortId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(clock),
        navkontor = navkontor,
        ikkeRettTilTiltakspengerTidspunkt = null,
        brukersMeldekort = brukersMeldekort,
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
