package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.overlappendePerioder
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.felles.Navkontor
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface MeldekortBehandling {
    val id: MeldekortId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val opprettet: LocalDateTime
    val beregning: MeldeperiodeBeregning
    val meldeperiode: Meldeperiode

    // TODO abn: vi trenger kanskje ikke denne?
    val type: MeldekortBehandlingType

    /** Vil kunne være null dersom vi ikke har mottatt et meldekort via vår digitale flate. Bør på sikt kunne være en liste? */
    val brukersMeldekort: BrukersMeldekort?

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

    val ordinærBeløp: Int?
    val barnetilleggBeløp: Int?

    val kjedeId: MeldeperiodeKjedeId get() = meldeperiode.kjedeId

    fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): MeldekortBehandling

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den vil bli erstattet med AVBRUTT senere. */
    val erAvsluttet
        get() = when (status) {
            IKKE_BEHANDLET, KLAR_TIL_BESLUTNING -> false
            GODKJENT, IKKE_RETT_TIL_TILTAKSPENGER -> true
        }

    /** Merk at statusen [IKKE_RETT_TIL_TILTAKSPENGER] anses som avsluttet. Den vil bli erstattet med AVBRUTT senere. */
    fun erÅpen(): Boolean = !erAvsluttet

    /** Oppdaterer meldeperioden til [meldeperiode] dersom den har samme kjede id, den er nyere enn den eksisterende og dette ikke er avsluttet meldekortbehandling. */
    fun oppdaterMeldeperiode(
        meldeperiode: Meldeperiode,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
    ): MeldekortBehandling? {
        require(meldeperiode.kjedeId == kjedeId) {
            "MeldekortBehandling: Kan ikke oppdatere meldeperiode med annen kjede id. ${meldeperiode.kjedeId} != $kjedeId"
        }
        if (erAvsluttet) return null
        if (meldeperiode.versjon <= this.meldeperiode.versjon) return null

        val ikkeRettTilTiltakspengerTidspunkt = if (meldeperiode.ingenDagerGirRett) nå() else null
        return when (this) {
            is MeldekortBehandlet -> this.tilUnderBehandling(
                nyMeldeperiode = meldeperiode,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                tiltakstypePerioder = tiltakstypePerioder,
            )

            is MeldekortUnderBehandling -> this.copy(
                meldeperiode = meldeperiode,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                    meldeperiode = meldeperiode,
                    meldekortId = this.id,
                    sakId = this.sakId,
                    tiltakstypePerioder = tiltakstypePerioder,
                ),
            )
        }
    }

    /**
     * Meldekort utfylt av saksbehandler og godkjent av beslutter.
     * Når veileder/bruker har fylt ut meldekortet vil ikke denne klassen kunne gjenbrukes uten endringer. Kanskje vi må ha en egen klasse for veileder-/brukerutfylt meldekort.
     *
     * @param saksbehandler: Obligatorisk dersom meldekortet er utfylt av saksbehandler.
     * @param beslutter: Obligatorisk dersom meldekortet er godkjent av beslutter.
     */
    data class MeldekortBehandlet(
        override val id: MeldekortId,
        override val sakId: SakId,
        override val saksnummer: Saksnummer,
        override val fnr: Fnr,
        override val opprettet: LocalDateTime,
        override val beregning: MeldeperiodeBeregning.UtfyltMeldeperiode,
        override val saksbehandler: String,
        override val sendtTilBeslutning: LocalDateTime?,
        override val beslutter: String?,
        override val status: MeldekortBehandlingStatus,
        override val iverksattTidspunkt: LocalDateTime?,
        override val navkontor: Navkontor,
        override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
        override val brukersMeldekort: BrukersMeldekort?,
        override val meldeperiode: Meldeperiode,
        override val type: MeldekortBehandlingType,
    ) : MeldekortBehandling {

        init {
            require(meldeperiode.periode == periode)
            require(beregning.periode == periode)
            when (status) {
                IKKE_BEHANDLET -> throw IllegalStateException("Et utfylt meldekort kan ikke ha status IKKE_UTFYLT")
                KLAR_TIL_BESLUTNING -> {
                    require(iverksattTidspunkt == null)
                    // Kommentar jah: Når vi legger til underkjenn, bør vi også legge til et atteserings objekt som for Behandling. beslutter vil da flyttes dit.
                    requireNotNull(sendtTilBeslutning)
                    require(beslutter == null)
                }

                GODKJENT -> {
                    require(ikkeRettTilTiltakspengerTidspunkt == null)
                    requireNotNull(iverksattTidspunkt)
                    requireNotNull(beslutter)
                    requireNotNull(sendtTilBeslutning)
                }

                IKKE_RETT_TIL_TILTAKSPENGER -> {
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
            require(status == KLAR_TIL_BESLUTNING)
            require(this.beslutter == null)
            return this.copy(
                beslutter = beslutter.navIdent,
                status = GODKJENT,
                iverksattTidspunkt = nå(),
            ).right()
        }

        override fun settIkkeRettTilTiltakspenger(
            periode: Periode,
            tidspunkt: LocalDateTime,
        ): MeldekortBehandlet {
            throw IllegalStateException("I førsteomgang støtter vi kun stans av ikke-utfylte meldekort.")
        }

        fun tilUnderBehandling(
            nyMeldeperiode: Meldeperiode?,
            ikkeRettTilTiltakspengerTidspunkt: LocalDateTime? = null,
            tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
        ): MeldekortUnderBehandling {
            val meldeperiode = nyMeldeperiode ?: this.meldeperiode
            return MeldekortUnderBehandling(
                id = this.id,
                sakId = this.sakId,
                saksnummer = this.saksnummer,
                fnr = this.fnr,
                opprettet = this.opprettet,
                beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                    meldeperiode = meldeperiode,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldekortId = this.id,
                    sakId = this.sakId,
                ),
                saksbehandler = saksbehandler,
                navkontor = this.navkontor,
                ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
                type = type,
            )
        }

        override val beløpTotal: Int = beregning.beregnTotaltBeløp()
        override val ordinærBeløp: Int = beregning.beregnTotalOrdinærBeløp()
        override val barnetilleggBeløp: Int = beregning.beregnTotalBarnetiillegg()

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
        override val opprettet: LocalDateTime,
        override val beregning: MeldeperiodeBeregning.IkkeUtfyltMeldeperiode,
        override val navkontor: Navkontor,
        override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
        override val brukersMeldekort: BrukersMeldekort?,
        override val meldeperiode: Meldeperiode,
        override val saksbehandler: String,
        override val type: MeldekortBehandlingType,
    ) : MeldekortBehandling {
        override val iverksattTidspunkt = null
        override val sendtTilBeslutning = null

        override val beløpTotal = null
        override val ordinærBeløp = null
        override val barnetilleggBeløp = null
        override val status =
            if (ikkeRettTilTiltakspengerTidspunkt == null) IKKE_BEHANDLET else IKKE_RETT_TIL_TILTAKSPENGER

        override val beslutter = null

        fun sendTilBeslutter(
            utfyltMeldeperiode: MeldeperiodeBeregning.UtfyltMeldeperiode,
            saksbehandler: Saksbehandler,
        ): Either<KanIkkeSendeMeldekortTilBeslutning, MeldekortBehandlet> {
            require(utfyltMeldeperiode.periode == this.periode) {
                "Når man fyller ut et meldekort må meldekortperioden være den samme som den som er opprettet. Opprettet periode: ${this.beregning.periode}, utfylt periode: ${utfyltMeldeperiode.periode}"
            }
            require(sakId == utfyltMeldeperiode.sakId)
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
                beregning = utfyltMeldeperiode,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = nå(),
                beslutter = this.beslutter,
                status = KLAR_TIL_BESLUTNING,
                iverksattTidspunkt = null,
                navkontor = this.navkontor,
                ikkeRettTilTiltakspengerTidspunkt = null,
                brukersMeldekort = brukersMeldekort,
                meldeperiode = meldeperiode,
                type = type,
            ).right()
        }

        fun erKlarTilUtfylling(): Boolean {
            return !LocalDate.now().isBefore(periode.fraOgMed)
        }

        // TODO John og Anders: I dette tilfellet er det bedre og avbryte behandlingen, men da må vi lage et avbrutt behandling konsept.
        override fun settIkkeRettTilTiltakspenger(
            periode: Periode,
            tidspunkt: LocalDateTime,
        ): MeldekortUnderBehandling {
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
            if (status == IKKE_RETT_TIL_TILTAKSPENGER) {
                require(beregning.dager.all { it is MeldeperiodeBeregningDag.Utfylt.Sperret })
            }
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
): MeldekortBehandling.MeldekortUnderBehandling {
    require(this.vedtaksliste.innvilgelsesperioder.isNotEmpty()) {
        "Må ha minst én periode som gir rett til tiltakspenger for å opprette meldekortbehandling"
    }

    val forrigeMeldekortBehandling = hentSisteMeldekortBehandlingForKjede(kjedeId)?.also {
        require(it.status == GODKJENT) {
            "Forrige meldekortbehandling i kjeden må være godkjent for å opprette en ny behandling/korrigering (kjede $kjedeId på sak ${this.id})"
        }
    }
    val meldekortId = MeldekortId.random()
    val meldeperiode = hentSisteMeldeperiodeForKjede(kjedeId)

    val overlappendePeriode = this.vedtaksliste.innvilgelsesperioder.overlappendePerioder(
        listOf(meldeperiode.periode),
    ).singleOrNullOrThrow()

    requireNotNull(overlappendePeriode) { "Meldeperioden må overlappe med innvilgelsesperioden(e)" }

    // TODO abn: må støtte flere brukers meldekort på samme kjede før vi åpner for korrigering fra bruker
    val brukersMeldekort = this.brukersMeldekort.find { it.kjedeId == kjedeId }

    val type =
        if (forrigeMeldekortBehandling == null) MeldekortBehandlingType.FØRSTE_BEHANDLING else MeldekortBehandlingType.KORRIGERING

    return MeldekortBehandling.MeldekortUnderBehandling(
        id = meldekortId,
        sakId = this.id,
        saksnummer = this.saksnummer,
        fnr = this.fnr,
        opprettet = nå(),
        navkontor = navkontor,
        ikkeRettTilTiltakspengerTidspunkt = null,
        brukersMeldekort = brukersMeldekort,
        meldeperiode = meldeperiode,
        saksbehandler = saksbehandler.navIdent,
        type = type,
        beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
            meldeperiode = meldeperiode,
            meldekortId = meldekortId,
            sakId = this.id,
            tiltakstypePerioder = this.vedtaksliste.tiltakstypeperioder,
        ),
    )
}
