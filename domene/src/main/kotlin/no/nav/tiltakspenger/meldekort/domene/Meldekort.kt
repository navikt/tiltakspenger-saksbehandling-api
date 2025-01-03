package no.nav.tiltakspenger.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

sealed interface Meldekort {
    val id: MeldekortId
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val rammevedtakId: VedtakId
    val forrigeMeldekortId: MeldekortId?
    val opprettet: LocalDateTime
    val meldeperiode: Meldeperiode

    /** Et vedtak kan føre til at en meldeperiode ikke lenger gir rett til tiltakspenger; vil den da ha en tiltakstype? */
    val tiltakstype: TiltakstypeSomGirRett
    val fraOgMed: LocalDate get() = meldeperiode.fraOgMed
    val tilOgMed: LocalDate get() = meldeperiode.tilOgMed
    val periode: Periode get() = meldeperiode.periode
    val saksbehandler: String?
    val beslutter: String?
    val status: MeldekortStatus
    val navkontor: Navkontor?
    val iverksattTidspunkt: LocalDateTime?
    val sendtTilBeslutning: LocalDateTime?
    val sendtTilMeldekortApi: LocalDateTime?

    /** Denne styres kun av vedtakene. Dersom vi har en åpen meldekortbehandling (inkl. til beslutning) kan et nytt vedtak overstyre hele meldeperioden til [MeldekortStatus.IKKE_RETT_TIL_TILTAKSPENGER] */
    val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?

    /** Totalsummen for meldeperioden */
    val beløpTotal: Int?

    val meldeperiodeId: MeldeperiodeId get() = MeldeperiodeId.fraPeriode(periode)

    fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): Meldekort

    /**
     * Meldekort utfylt av saksbehandler og godkjent av beslutter.
     * Når veileder/bruker har fylt ut meldekortet vil ikke denne klassen kunne gjenbrukes uten endringer. Kanskje vi må ha en egen klasse for veileder-/brukerutfylt meldekort.
     *
     * @param saksbehandler: Obligatorisk dersom meldekortet er utfylt av saksbehandler.
     * @param beslutter: Obligatorisk dersom meldekortet er godkjent av beslutter.
     * @param forrigeMeldekortId kan være null dersom det er første meldekort.
     */
    data class UtfyltMeldekort(
        override val id: MeldekortId,
        override val meldeperiodeId: MeldeperiodeId,
        override val sakId: SakId,
        override val saksnummer: Saksnummer,
        override val fnr: Fnr,
        override val rammevedtakId: VedtakId,
        override val forrigeMeldekortId: MeldekortId?,
        override val opprettet: LocalDateTime,
        override val meldeperiode: Meldeperiode.UtfyltMeldeperiode,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val saksbehandler: String,
        override val sendtTilBeslutning: LocalDateTime?,
        override val beslutter: String?,
        override val status: MeldekortStatus,
        override val iverksattTidspunkt: LocalDateTime?,
        override val navkontor: Navkontor,
        override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
        override val sendtTilMeldekortApi: LocalDateTime?,
    ) : Meldekort {

        init {
            when (status) {
                MeldekortStatus.IKKE_UTFYLT -> throw IllegalStateException("Et utfylt meldekort kan ikke ha status IKKE_UTFYLT")
                MeldekortStatus.KLAR_TIL_BESLUTNING -> {
                    require(iverksattTidspunkt == null)
                    // Kommentar jah: Når vi legger til underkjenn, bør vi også legge til et atteserings objekt som for Behandling. beslutter vil da flyttes dit.
                    requireNotNull(sendtTilBeslutning)
                    require(beslutter == null)
                }

                MeldekortStatus.GODKJENT -> {
                    require(ikkeRettTilTiltakspengerTidspunkt == null)
                    requireNotNull(iverksattTidspunkt)
                    requireNotNull(beslutter)
                    requireNotNull(sendtTilBeslutning)
                }

                MeldekortStatus.IKKE_RETT_TIL_TILTAKSPENGER -> {
                    throw IllegalStateException("I førsteomgang støtter vi kun stans av ikke-utfylte meldekort.")
                    // require(iverksattTidspunkt == null)
                    // require(beslutter == null)
                    // require(sendtTilBeslutning == null)
                }
            }
        }

        /**
         * TODO post-mvp jah: Ved revurderinger av rammevedtaket, så må vi basere oss på både forrige meldekort og revurderingsvedtaket. Dette løser vi å flytte mer logikk til Sak.kt.
         * TODO post-mvp jah: Når vi implementerer delvis innvilgelse vil hele meldekortperioder kunne bli SPERRET.
         */
        fun opprettNesteMeldekort(
            utfallsperioder: Periodisering<AvklartUtfallForPeriode>,
        ): Either<SisteMeldekortErUtfylt, IkkeUtfyltMeldekort> {
            val periode = Periode(fraOgMed.plusDays(14), tilOgMed.plusDays(14))
            if (periode.tilOgMed.isAfter(utfallsperioder.totalePeriode.tilOgMed)) {
                return SisteMeldekortErUtfylt.left()
            }
            val meldekortId = MeldekortId.random()
            return IkkeUtfyltMeldekort(
                id = meldekortId,
                meldeperiodeId = MeldeperiodeId.fraPeriode(periode),
                sakId = this.sakId,
                saksnummer = this.saksnummer,
                fnr = this.fnr,
                rammevedtakId = this.rammevedtakId,
                forrigeMeldekortId = this.id,
                opprettet = nå(),
                tiltakstype = this.tiltakstype,
                navkontor = this.navkontor,
                meldeperiode = Meldeperiode.IkkeUtfyltMeldeperiode.fraPeriode(
                    meldeperiode = periode,
                    tiltakstype = this.tiltakstype,
                    meldekortId = meldekortId,
                    sakId = this.sakId,
                    utfallsperioder = utfallsperioder,
                    maksDagerMedTiltakspengerForPeriode = this.meldeperiode.maksDagerMedTiltakspengerForPeriode,
                ),
                ikkeRettTilTiltakspengerTidspunkt = null,
            ).right()
        }

        fun iverksettMeldekort(
            beslutter: Saksbehandler,
        ): Either<KanIkkeIverksetteMeldekort, UtfyltMeldekort> {
            if (!beslutter.erBeslutter()) {
                return KanIkkeIverksetteMeldekort.MåVæreBeslutter(beslutter.roller).left()
            }
            if (saksbehandler == beslutter.navIdent) {
                return KanIkkeIverksetteMeldekort.SaksbehandlerOgBeslutterKanIkkeVæreLik.left()
            }
            require(status == MeldekortStatus.KLAR_TIL_BESLUTNING)
            require(this.beslutter == null)
            return this.copy(
                beslutter = beslutter.navIdent,
                status = MeldekortStatus.GODKJENT,
                iverksattTidspunkt = nå(),
            ).right()
        }

        override fun settIkkeRettTilTiltakspenger(
            periode: Periode,
            tidspunkt: LocalDateTime,
        ): UtfyltMeldekort {
            throw IllegalStateException("I førsteomgang støtter vi kun stans av ikke-utfylte meldekort.")
        }

        override val beløpTotal: Int = meldeperiode.beregnTotalbeløp()

        /** Finner den siste dagen i meldekortet som har beløp > 0. */
        val sisteUtbetalingsdag: LocalDate? by lazy {
            meldeperiode.dager.filter { it.beløp > 0 }.maxOfOrNull { it.dato }
        }
    }

    data class IkkeUtfyltMeldekort(
        override val id: MeldekortId,
        override val meldeperiodeId: MeldeperiodeId,
        override val sakId: SakId,
        override val saksnummer: Saksnummer,
        override val fnr: Fnr,
        override val rammevedtakId: VedtakId,
        override val forrigeMeldekortId: MeldekortId?,
        override val opprettet: LocalDateTime,
        override val tiltakstype: TiltakstypeSomGirRett,
        override val meldeperiode: Meldeperiode.IkkeUtfyltMeldeperiode,
        override val navkontor: Navkontor?,
        override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
    ) : Meldekort {
        override val iverksattTidspunkt = null
        override val sendtTilBeslutning = null
        override val sendtTilMeldekortApi = null

        override val beløpTotal = null
        override val status =
            if (ikkeRettTilTiltakspengerTidspunkt == null) MeldekortStatus.IKKE_UTFYLT else MeldekortStatus.IKKE_RETT_TIL_TILTAKSPENGER

        fun sendTilBeslutter(
            utfyltMeldeperiode: Meldeperiode.UtfyltMeldeperiode,
            saksbehandler: Saksbehandler,
            navkontor: Navkontor,
        ): Either<KanIkkeSendeMeldekortTilBeslutter, UtfyltMeldekort> {
            require(utfyltMeldeperiode.periode == this.periode) {
                "Når man fyller ut et meldekort må meldekortperioden være den samme som den som er opprettet. Opprettet periode: ${this.meldeperiode.periode}, utfylt periode: ${utfyltMeldeperiode.periode}"
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
            return UtfyltMeldekort(
                id = this.id,
                meldeperiodeId = this.meldeperiodeId,
                sakId = this.sakId,
                saksnummer = this.saksnummer,
                fnr = this.fnr,
                rammevedtakId = this.rammevedtakId,
                forrigeMeldekortId = this.forrigeMeldekortId,
                opprettet = this.opprettet,
                meldeperiode = utfyltMeldeperiode,
                tiltakstype = this.tiltakstype,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = nå(),
                beslutter = this.beslutter,
                status = MeldekortStatus.KLAR_TIL_BESLUTNING,
                iverksattTidspunkt = null,
                navkontor = navkontor,
                ikkeRettTilTiltakspengerTidspunkt = null,
                sendtTilMeldekortApi = this.sendtTilMeldekortApi,
            ).right()
        }

        override val beslutter = null

        override val saksbehandler = null

        fun erKlarTilUtfylling(): Boolean {
            return !LocalDate.now().isBefore(periode.fraOgMed)
        }

        override fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): IkkeUtfyltMeldekort {
            if (!periode.overlapperMed(this.periode)) {
                // Hvis periodene ikke overlapper blir det ingen endringer.
                return this
            }
            if (periode.inneholderHele(this.periode)) {
                // Hvis den nye vedtaksperioden dekker hele meldeperioden, setter vi alle dagene til SPERRET og hele meldekortet til IKKE_RETT_TIL_TILTAKSPENGER.
                return this.copy(
                    ikkeRettTilTiltakspengerTidspunkt = tidspunkt,
                    meldeperiode = meldeperiode.settAlleDagerTilSperret(),
                )
            }
            // Delvis overlapp, vi setter kun de dagene som overlapper til SPERRET.
            return this.copy(
                meldeperiode = meldeperiode.settPeriodeTilSperret(periode),
            )
        }

        init {
            if (status == MeldekortStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                require(meldeperiode.dager.all { it is Meldekortdag.Utfylt.Sperret })
            }
        }
    }
}

fun Rammevedtak.opprettFørsteMeldekortForEnSak(): Meldekort.IkkeUtfyltMeldekort {
    val periode = finnFørsteMeldekortsperiode(this.periode)
    val meldekortId = MeldekortId.random()
    val tiltakstype = this.behandling.vilkårssett.tiltakDeltagelseVilkår.registerSaksopplysning.tiltakstype
    val utfallsperioder: Periodisering<AvklartUtfallForPeriode> = this.utfallsperioder

    return Meldekort.IkkeUtfyltMeldekort(
        id = meldekortId,
        meldeperiodeId = MeldeperiodeId.fraPeriode(periode),
        sakId = this.sakId,
        saksnummer = this.saksnummer,
        fnr = this.behandling.fnr,
        rammevedtakId = this.id,
        forrigeMeldekortId = null,
        opprettet = nå(),
        tiltakstype = tiltakstype,
        // TODO post-mvp: Her har vi mulighet til å hente verdien fra brukers geografiske tilhørighet + norg2.
        navkontor = null,
        meldeperiode = Meldeperiode.IkkeUtfyltMeldeperiode.fraPeriode(
            meldeperiode = periode,
            utfallsperioder = utfallsperioder,
            tiltakstype = tiltakstype,
            meldekortId = meldekortId,
            sakId = this.sakId,
            maksDagerMedTiltakspengerForPeriode = this.behandling.maksDagerMedTiltakspengerForPeriode,
        ),
        ikkeRettTilTiltakspengerTidspunkt = null,
    )
}

fun finnFørsteMeldekortsperiode(periode: Periode): Periode {
    val førsteMandagIMeldekortsperiode = periode.fraOgMed.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sisteSøndagIMeldekortsperiode = førsteMandagIMeldekortsperiode.plusDays(13)

    return Periode(førsteMandagIMeldekortsperiode, sisteSøndagIMeldekortsperiode)
}
