package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.KanIkkeIverksetteMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.underkjenn.KanIkkeUnderkjenneMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDateTime

/**
 * Tilstandene (KLAR_TIL_BESLUTNING, UNDER_BESLUTNING, GODKJENT)
 * Meldekort utfylt av saksbehandler og sendt til beslutning eller godkjent av beslutter.
 *
 * @param saksbehandler Obligatorisk dersom meldekortet er utfylt av saksbehandler.
 * @param beslutter Obligatorisk dersom meldekortet er godkjent av beslutter.
 */
data class MeldekortbehandlingManuell(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val saksbehandler: String,
    override val sendtTilBeslutning: LocalDateTime?,
    override val beslutter: String?,
    override val status: MeldekortbehandlingStatus,
    override val iverksattTidspunkt: LocalDateTime?,
    override val navkontor: Navkontor,
    override val type: MeldekortbehandlingType,
    override val begrunnelse: Begrunnelse?,
    override val attesteringer: Attesteringer,
    override val simulering: Simulering?,
    override val sistEndret: LocalDateTime,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val skalSendeVedtaksbrev: Boolean,
    override val meldeperioder: Meldeperiodebehandlinger,
) : Meldekortbehandling.Behandlet {
    override val avbrutt: Avbrutt? = null
    override val beregning: Beregning get() = meldeperioder.beregning!!

    init {
        require(meldeperioder.fraOgMed == beregningPeriode.fraOgMed) {
            "Fra og med dato for beregningsperioden og meldeperioden må være like"
        }
        require(meldeperioder.tilOgMed <= beregningPeriode.tilOgMed) {
            "Til og med dato for beregningsperioden må være nyere eller lik meldeperioden"
        }
        when (status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            MeldekortbehandlingStatus.UNDER_BEHANDLING,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.AVBRUTT,
            -> throw IllegalStateException("Manuelt behandlede meldekort kan ikke ha status $status")

            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
                require(iverksattTidspunkt == null)
                requireNotNull(sendtTilBeslutning)
                require(beslutter == null)
            }

            MeldekortbehandlingStatus.UNDER_BESLUTNING -> {
                require(iverksattTidspunkt == null)
                requireNotNull(sendtTilBeslutning)
                requireNotNull(beslutter)
            }

            MeldekortbehandlingStatus.GODKJENT -> {
                require(!ingenDagerGirRett)
                requireNotNull(iverksattTidspunkt)
                requireNotNull(beslutter)
                requireNotNull(sendtTilBeslutning)
            }
        }
    }

    fun iverksettMeldekort(
        beslutter: Saksbehandler,
        clock: Clock,
    ): Either<KanIkkeIverksetteMeldekortbehandling, MeldekortbehandlingManuell> {
        if (saksbehandler == beslutter.navIdent) {
            return KanIkkeIverksetteMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreLik.left()
        }
        require(status == MeldekortbehandlingStatus.UNDER_BESLUTNING) {
            return KanIkkeIverksetteMeldekortbehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        require(this.beslutter == beslutter.navIdent) {
            return KanIkkeIverksetteMeldekortbehandling.MåVæreBeslutterForMeldekortet.left()
        }

        val attesteringer = this.attesteringer.leggTil(
            Attestering(
                id = AttesteringId.random(),
                status = Attesteringsstatus.GODKJENT,
                begrunnelse = null,
                beslutter = beslutter.navIdent,
                tidspunkt = LocalDateTime.now(clock),
            ),
        )

        return this.copy(
            beslutter = beslutter.navIdent,
            status = MeldekortbehandlingStatus.GODKJENT,
            iverksattTidspunkt = nå(clock),
            attesteringer = attesteringer,
            sistEndret = nå(clock),
        ).right()
    }

    fun underkjenn(
        besluttersBegrunnelse: NonBlankString,
        beslutter: Saksbehandler,
        clock: Clock,
    ): Either<KanIkkeUnderkjenneMeldekortbehandling, MeldekortUnderBehandling> {
        if (this.status != MeldekortbehandlingStatus.UNDER_BESLUTNING) {
            return KanIkkeUnderkjenneMeldekortbehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        if (this.saksbehandler == beslutter.navIdent) {
            return KanIkkeUnderkjenneMeldekortbehandling.SaksbehandlerKanIkkeUnderkjenneSinEgenBehandling.left()
        }
        if (this.iverksattTidspunkt != null) {
            return KanIkkeUnderkjenneMeldekortbehandling.BehandlingenErAlleredeBesluttet.left()
        }
        require(this.beslutter == beslutter.navIdent) {
            return KanIkkeUnderkjenneMeldekortbehandling.MåVæreBeslutterForMeldekortet.left()
        }

        val attesteringer = this.attesteringer.leggTil(
            Attestering(
                id = AttesteringId.random(),
                status = Attesteringsstatus.SENDT_TILBAKE,
                begrunnelse = besluttersBegrunnelse,
                beslutter = beslutter.navIdent,
                tidspunkt = LocalDateTime.now(clock),
            ),
        )

        return MeldekortUnderBehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            navkontor = navkontor,
            saksbehandler = saksbehandler,
            type = type,
            attesteringer = attesteringer,
            begrunnelse = begrunnelse,
            simulering = simulering,
            sendtTilBeslutning = sendtTilBeslutning,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
            sistEndret = nå(clock),
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
            meldeperioder = this.meldeperioder,
        ).right()
    }

    override fun overta(
        saksbehandler: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Meldekortbehandling> {
        return when (this.status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING -> throw IllegalStateException("Et manuelt behandlet meldekort kan ikke ha status KLAR_TIL_BEHANDLING")

            MeldekortbehandlingStatus.AVBRUTT -> throw IllegalStateException("Et manuelt behandlet meldekort kan ikke ha status AVBRUTT")

            MeldekortbehandlingStatus.UNDER_BEHANDLING -> throw IllegalStateException("Et utfylt meldekort kan ikke ha status UNDER_BEHANDLING")

            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> KunneIkkeOvertaMeldekortbehandling.BehandlingenMåVæreUnderBeslutningForÅOverta.left()

            MeldekortbehandlingStatus.UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                if (this.beslutter == null) {
                    return KunneIkkeOvertaMeldekortbehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta.left()
                }
                if (this.saksbehandler == saksbehandler.navIdent) {
                    return KunneIkkeOvertaMeldekortbehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme.left()
                }
                this.copy(
                    beslutter = saksbehandler.navIdent,
                    sistEndret = nå(clock),
                ).right()
            }

            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            -> KunneIkkeOvertaMeldekortbehandling.BehandlingenKanIkkeVæreGodkjentEllerIkkeRett.left()
        }
    }

    override fun taMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        return when (this.status) {
            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == null) { "Meldekortbehandlingen har en eksisterende beslutter. For å overta meldekortbehandlingen, bruk overta() - meldekortId: ${this.id}" }
                this.copy(
                    beslutter = saksbehandler.navIdent,
                    status = MeldekortbehandlingStatus.UNDER_BESLUTNING,
                    sistEndret = nå(clock),
                )
            }

            MeldekortbehandlingStatus.UNDER_BEHANDLING,
            MeldekortbehandlingStatus.UNDER_BESLUTNING,
            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.AVBRUTT,
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke ta meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun leggTilbakeMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        return when (this.status) {
            MeldekortbehandlingStatus.UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == saksbehandler.navIdent)
                this.copy(
                    beslutter = null,
                    status = MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
                    sistEndret = nå(clock),
                )
            }

            MeldekortbehandlingStatus.UNDER_BEHANDLING,
            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.AVBRUTT,
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke legge tilbake meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    override fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling {
        throw IllegalStateException("Kan ikke oppdatere simulering for status $status. SakId: $sakId, meldekortId: $id")
    }

    fun tilUnderBehandling(
        nyeMeldeperioder: Meldeperiodebehandlinger,
        tidspunkt: LocalDateTime,
    ): MeldekortUnderBehandling {
        when (this.status) {
            MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING,
            MeldekortbehandlingStatus.UNDER_BEHANDLING,
            MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING,
            MeldekortbehandlingStatus.UNDER_BESLUTNING,
            -> Unit

            MeldekortbehandlingStatus.GODKJENT,
            MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET,
            MeldekortbehandlingStatus.AVBRUTT,
            -> throw IllegalStateException("Kan ikke gå fra GODKJENT, AUTOMATISK_BEHANDLET, AVBRUTT eller IKKE_RETT_TIL_TILTAKSPENGER til UNDER_BEHANDLING")
        }

        return MeldekortUnderBehandling(
            id = this.id,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            opprettet = this.opprettet,
            saksbehandler = saksbehandler,
            navkontor = this.navkontor,
            type = type,
            begrunnelse = this.begrunnelse,
            attesteringer = attesteringer,
            sendtTilBeslutning = iverksattTidspunkt,
            simulering = null,
            status = MeldekortbehandlingStatus.UNDER_BEHANDLING,
            sistEndret = tidspunkt,
            fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev,
            meldeperioder = nyeMeldeperioder,
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        )
    }
}
