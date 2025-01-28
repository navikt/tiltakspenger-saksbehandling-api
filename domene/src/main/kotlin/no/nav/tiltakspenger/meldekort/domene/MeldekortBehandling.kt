package no.nav.tiltakspenger.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface MeldekortBehandling {
    val id: MeldekortId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr

    // TODO: slette?
    val rammevedtakId: VedtakId

    // TODO: slette?
    val forrigeMeldekortId: MeldekortId?

    val opprettet: LocalDateTime
    val beregning: MeldeperiodeBeregning

    val meldeperiode: Meldeperiode

    // I tilfeller saksbehandler har startet behandling uten brukers meldekort/rapportering
    // Gjelder spesielt da bruker rapporterte via arena (men manuell kopier av saksbehandler)
    val brukersMeldekort: BrukersMeldekort?

    /** Et vedtak kan føre til at en meldeperiode ikke lenger gir rett til tiltakspenger; vil den da ha en tiltakstype? */
    val tiltakstype: TiltakstypeSomGirRett
    val fraOgMed: LocalDate get() = beregning.fraOgMed
    val tilOgMed: LocalDate get() = beregning.tilOgMed
    val periode: Periode get() = beregning.periode
    val saksbehandler: String
    val beslutter: String?
    val status: MeldekortBehandlingStatus
    val navkontor: Navkontor
    val iverksattTidspunkt: LocalDateTime?
    val sendtTilBeslutning: LocalDateTime?

    /** Denne styres kun av vedtakene. Dersom vi har en åpen meldekortbehandling (inkl. til beslutning) kan et nytt vedtak overstyre hele meldeperioden til [MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER] */
    val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?

    /** Totalsummen for meldeperioden */
    val beløpTotal: Int?

    val meldeperiodeKjedeId: MeldeperiodeKjedeId get() = meldeperiode.meldeperiodeKjedeId

    fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): MeldekortBehandling

    /**
     * Meldekort utfylt av saksbehandler og godkjent av beslutter.
     * Når veileder/bruker har fylt ut meldekortet vil ikke denne klassen kunne gjenbrukes uten endringer. Kanskje vi må ha en egen klasse for veileder-/brukerutfylt meldekort.
     *
     * @param saksbehandler: Obligatorisk dersom meldekortet er utfylt av saksbehandler.
     * @param beslutter: Obligatorisk dersom meldekortet er godkjent av beslutter.
     * @param forrigeMeldekortId kan være null dersom det er første meldekort.
     */
    data class MeldekortBehandlet(
        override val id: MeldekortId,
        override val sakId: SakId,
        override val saksnummer: Saksnummer,
        override val fnr: Fnr,
        override val rammevedtakId: VedtakId,
        override val forrigeMeldekortId: MeldekortId?,
        override val opprettet: LocalDateTime,
        override val beregning: MeldeperiodeBeregning.UtfyltMeldeperiode,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val saksbehandler: String,
        override val sendtTilBeslutning: LocalDateTime?,
        override val beslutter: String?,
        override val status: MeldekortBehandlingStatus,
        override val iverksattTidspunkt: LocalDateTime?,
        override val navkontor: Navkontor,
        override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
        override val brukersMeldekort: BrukersMeldekort?,
        override val meldeperiode: Meldeperiode,
    ) : MeldekortBehandling {

        init {
            require(meldeperiode.periode == periode)
            require(beregning.periode == periode)
            when (status) {
                MeldekortBehandlingStatus.IKKE_BEHANDLET -> throw IllegalStateException("Et utfylt meldekort kan ikke ha status IKKE_UTFYLT")
                MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING -> {
                    require(iverksattTidspunkt == null)
                    // Kommentar jah: Når vi legger til underkjenn, bør vi også legge til et atteserings objekt som for Behandling. beslutter vil da flyttes dit.
                    requireNotNull(sendtTilBeslutning)
                    require(beslutter == null)
                }

                MeldekortBehandlingStatus.GODKJENT -> {
                    require(ikkeRettTilTiltakspengerTidspunkt == null)
                    requireNotNull(iverksattTidspunkt)
                    requireNotNull(beslutter)
                    requireNotNull(sendtTilBeslutning)
                }

                MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
                    throw IllegalStateException("I førsteomgang støtter vi kun stans av ikke-utfylte meldekort.")
                    // require(iverksattTidspunkt == null)
                    // require(beslutter == null)
                    // require(sendtTilBeslutning == null)
                }
            }
        }

        fun iverksettMeldekort(
            beslutter: Saksbehandler,
        ): Either<KanIkkeIverksetteMeldekort, MeldekortBehandlet> {
            if (!beslutter.erBeslutter()) {
                return KanIkkeIverksetteMeldekort.MåVæreBeslutter(beslutter.roller).left()
            }
            if (saksbehandler == beslutter.navIdent) {
                return KanIkkeIverksetteMeldekort.SaksbehandlerOgBeslutterKanIkkeVæreLik.left()
            }
            require(status == MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING)
            require(this.beslutter == null)
            return this.copy(
                beslutter = beslutter.navIdent,
                status = MeldekortBehandlingStatus.GODKJENT,
                iverksattTidspunkt = nå(),
            ).right()
        }

        override fun settIkkeRettTilTiltakspenger(
            periode: Periode,
            tidspunkt: LocalDateTime,
        ): MeldekortBehandlet {
            throw IllegalStateException("I førsteomgang støtter vi kun stans av ikke-utfylte meldekort.")
        }

        override val beløpTotal: Int = beregning.beregnTotalbeløp()

        /** Finner den siste dagen i meldekortet som har beløp > 0. */
        val sisteUtbetalingsdag: LocalDate? by lazy {
            beregning.dager.filter { it.beløp > 0 }.maxOfOrNull { it.dato }
        }
    }

    data class MeldekortUnderBehandling(
        override val id: MeldekortId,
        override val sakId: SakId,
        override val saksnummer: Saksnummer,
        override val fnr: Fnr,
        override val rammevedtakId: VedtakId,
        override val forrigeMeldekortId: MeldekortId?,
        override val opprettet: LocalDateTime,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val beregning: MeldeperiodeBeregning.IkkeUtfyltMeldeperiode,
        override val navkontor: Navkontor,
        override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
        override val brukersMeldekort: BrukersMeldekort?,
        override val meldeperiode: Meldeperiode,
        override val saksbehandler: String,
    ) : MeldekortBehandling {
        override val iverksattTidspunkt = null
        override val sendtTilBeslutning = null

        override val beløpTotal = null
        override val status =
            if (ikkeRettTilTiltakspengerTidspunkt == null) MeldekortBehandlingStatus.IKKE_BEHANDLET else MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER

        override val beslutter = null

        fun sendTilBeslutter(
            utfyltMeldeperiode: MeldeperiodeBeregning.UtfyltMeldeperiode,
            saksbehandler: Saksbehandler,
        ): Either<KanIkkeSendeMeldekortTilBeslutter, MeldekortBehandlet> {
            require(utfyltMeldeperiode.periode == this.periode) {
                "Når man fyller ut et meldekort må meldekortperioden være den samme som den som er opprettet. Opprettet periode: ${this.beregning.periode}, utfylt periode: ${utfyltMeldeperiode.periode}"
            }
            require(sakId == utfyltMeldeperiode.sakId)
            if (!saksbehandler.erSaksbehandler()) {
                return KanIkkeSendeMeldekortTilBeslutter.MåVæreSaksbehandler(saksbehandler.roller).left()
            }
            if (!erKlarTilUtfylling()) {
                // John har avklart med Sølvi og Taulant at vi bør ha en begrensning på at vi kan fylle ut et meldekort hvis dagens dato er innenfor meldekortperioden eller senere.
                // Dette kan endres på ved behov.
                return KanIkkeSendeMeldekortTilBeslutter.MeldekortperiodenKanIkkeVæreFremITid.left()
            }
            return MeldekortBehandlet(
                id = this.id,
                sakId = this.sakId,
                saksnummer = this.saksnummer,
                fnr = this.fnr,
                rammevedtakId = this.rammevedtakId,
                forrigeMeldekortId = this.forrigeMeldekortId,
                opprettet = this.opprettet,
                beregning = utfyltMeldeperiode,
                tiltakstype = this.tiltakstype,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = nå(),
                beslutter = this.beslutter,
                status = MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING,
                iverksattTidspunkt = null,
                navkontor = this.navkontor,
                ikkeRettTilTiltakspengerTidspunkt = null,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
            ).right()
        }

        fun erKlarTilUtfylling(): Boolean {
            return !LocalDate.now().isBefore(periode.fraOgMed)
        }

        override fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): MeldekortUnderBehandling {
            if (!periode.overlapperMed(this.periode)) {
                // Hvis periodene ikke overlapper blir det ingen endringer.
                return this
            }
            if (periode.inneholderHele(this.periode)) {
                // Hvis den nye vedtaksperioden dekker hele meldeperioden, setter vi alle dagene til SPERRET og hele meldekortet til IKKE_RETT_TIL_TILTAKSPENGER.
                return this.copy(
                    ikkeRettTilTiltakspengerTidspunkt = tidspunkt,
                    beregning = beregning.settAlleDagerTilSperret(),
                )
            }
            // Delvis overlapp, vi setter kun de dagene som overlapper til SPERRET.
            return this.copy(
                beregning = beregning.settPeriodeTilSperret(periode),
            )
        }

        init {
            if (status == MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                require(beregning.dager.all { it is MeldeperiodeBeregningDag.Utfylt.Sperret })
            }
        }
    }
}

/**
 * (TODO'er fra tidligere implementasjon!)
 * TODO post-mvp jah: Ved revurderinger av rammevedtaket, så må vi basere oss på både forrige meldekort og revurderingsvedtaket. Dette løser vi å flytte mer logikk til Sak.kt.
 * TODO post-mvp jah: Når vi implementerer delvis innvilgelse vil hele meldekortperioder kunne bli SPERRET.
 */
fun Sak.opprettMeldekortBehandling(
    meldeperiode: Meldeperiode,
    navkontor: Navkontor,
    saksbehandler: Saksbehandler,
): MeldekortBehandling.MeldekortUnderBehandling {
    val meldekortId = MeldekortId.random()

    requireNotNull(this.vedtaksliste.vedtaksperiode) { "Må ha en vedtaksperiode for å opprette meldekortbehandling" }

    val overlappendePeriode = meldeperiode.periode.overlappendePeriode(this.vedtaksliste.vedtaksperiode)

    requireNotNull(overlappendePeriode) { "Meldeperioden må overlappe med vedtaksperioden" }

    // TODO: håndtere flere vedtak
    val vedtak = this.vedtaksliste.tidslinjeForPeriode(overlappendePeriode).single().verdi

    // TODO: hent tiltakstype fra gjeldende periode
    val tiltakstype = vedtak.behandling.tiltakstype

    // Dette blir vel feil dersom meldekort noen gang behandles i "feil" rekkefølge
    val forrigeMeldekortBehandling = this.meldekortBehandlinger.sisteGodkjenteMeldekort

    return MeldekortBehandling.MeldekortUnderBehandling(
        id = meldekortId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        rammevedtakId = vedtak.id,
        fnr = this.fnr,
        opprettet = nå(),
        forrigeMeldekortId = forrigeMeldekortBehandling?.id,
        navkontor = navkontor,
        ikkeRettTilTiltakspengerTidspunkt = null,
        brukersMeldekort = null,
        meldeperiode = meldeperiode,
        tiltakstype = tiltakstype,
        saksbehandler = saksbehandler.navIdent,
        beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
            meldeperiode = meldeperiode,
            tiltakstype = tiltakstype,
            meldekortId = meldekortId,
            sakId = this.id,
            utfallsperioder = this.vedtaksliste.utfallsperioder,
            maksDagerMedTiltakspengerForPeriode = vedtak.behandling.maksDagerMedTiltakspengerForPeriode,
        ),
    )
}
