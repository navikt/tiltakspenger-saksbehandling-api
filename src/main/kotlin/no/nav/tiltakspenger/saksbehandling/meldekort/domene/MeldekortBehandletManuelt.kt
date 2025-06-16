package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.GODKJENT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDateTime

/**
 * Meldekort utfylt av saksbehandler og sendt til beslutning eller godkjent av beslutter.
 * Når veileder/bruker har fylt ut meldekortet vil ikke denne klassen kunne gjenbrukes uten endringer. Kanskje vi må ha en egen klasse for veileder-/brukerutfylt meldekort.
 *
 * @param saksbehandler: Obligatorisk dersom meldekortet er utfylt av saksbehandler.
 * @param beslutter: Obligatorisk dersom meldekortet er godkjent av beslutter.
 */
data class MeldekortBehandletManuelt(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
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
    override val begrunnelse: MeldekortBehandlingBegrunnelse?,
    override val attesteringer: Attesteringer,
    override val beregning: MeldekortBeregning,
    override val simulering: Simulering?,
    override val dager: MeldekortDager,
) : MeldekortBehandling.Behandlet {
    override val avbrutt: Avbrutt? = null

    init {
        require(meldeperiode.periode.fraOgMed == beregningPeriode.fraOgMed) {
            "Fra og med dato for beregningsperioden og meldeperioden må være like"
        }
        require(meldeperiode.periode.tilOgMed <= beregningPeriode.tilOgMed) {
            "Til og med dato for beregningsperioden må være nyere eller lik meldeperioden"
        }
        when (status) {
            KLAR_TIL_BEHANDLING -> throw IllegalStateException("Et utfylt meldekort kan ikke ha status UNDER_BEHANDLING")
            KLAR_TIL_BESLUTNING -> {
                require(iverksattTidspunkt == null)
                // Kommentar jah: Når vi legger til underkjenn, bør vi også legge til et atteserings objekt som for Behandling. beslutter vil da flyttes dit.
                requireNotNull(sendtTilBeslutning)
                require(beslutter == null)
            }
            UNDER_BESLUTNING -> {
                require(iverksattTidspunkt == null)
                requireNotNull(sendtTilBeslutning)
                requireNotNull(beslutter)
            }
            GODKJENT -> {
                require(ikkeRettTilTiltakspengerTidspunkt == null)
                requireNotNull(iverksattTidspunkt)
                requireNotNull(beslutter)
                requireNotNull(sendtTilBeslutning)
            }

            IKKE_RETT_TIL_TILTAKSPENGER -> {
                throw IllegalStateException("I førsteomgang støtter vi kun stans av ikke-utfylte meldekort.")
            }

            AUTOMATISK_BEHANDLET -> throw IllegalStateException("Et manuelt behandlet meldekort kan ikke ha status AUTOMATISK_BEHANDLET")

            AVBRUTT -> throw IllegalStateException("Et manuelt behandlet meldekort kan ikke ha status AVBRUTT")
        }
    }

    fun iverksettMeldekort(
        beslutter: Saksbehandler,
        clock: Clock,
    ): Either<KanIkkeIverksetteMeldekort, MeldekortBehandletManuelt> {
        krevBeslutterRolle(beslutter)
        if (saksbehandler == beslutter.navIdent) {
            return KanIkkeIverksetteMeldekort.SaksbehandlerOgBeslutterKanIkkeVæreLik.left()
        }
        require(status == UNDER_BESLUTNING) {
            return KanIkkeIverksetteMeldekort.BehandlingenErIkkeUnderBeslutning.left()
        }
        require(this.beslutter == beslutter.navIdent) {
            return KanIkkeIverksetteMeldekort.MåVæreBeslutterForMeldekortet.left()
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
            status = GODKJENT,
            iverksattTidspunkt = nå(clock),
            attesteringer = attesteringer,
        ).right()
    }

    fun underkjenn(
        besluttersBegrunnelse: NonBlankString,
        beslutter: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeUnderkjenneMeldekortBehandling, MeldekortUnderBehandling> {
        if (this.status != UNDER_BESLUTNING) {
            return KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErIkkeUnderBeslutning.left()
        }
        if (this.saksbehandler == beslutter.navIdent) {
            return KunneIkkeUnderkjenneMeldekortBehandling.SaksbehandlerKanIkkeUnderkjenneSinEgenBehandling.left()
        }
        if (this.iverksattTidspunkt != null) {
            return KunneIkkeUnderkjenneMeldekortBehandling.BehandlingenErAlleredeBesluttet.left()
        }
        require(this.beslutter == beslutter.navIdent) {
            return KunneIkkeUnderkjenneMeldekortBehandling.MåVæreBeslutterForMeldekortet.left()
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
            beregning = beregning,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            saksbehandler = saksbehandler,
            type = type,
            attesteringer = attesteringer,
            begrunnelse = begrunnelse,
            simulering = simulering,
            sendtTilBeslutning = sendtTilBeslutning,
            dager = dager,
        ).right()
    }

    override fun overta(
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        return when (this.status) {
            AVBRUTT -> throw IllegalStateException("Et manuelt behandlet meldekort kan ikke ha status AVBRUTT")
            KLAR_TIL_BEHANDLING -> throw IllegalStateException("Et utfylt meldekort kan ikke ha status UNDER_BEHANDLING")
            KLAR_TIL_BESLUTNING -> KunneIkkeOvertaMeldekortBehandling.BehandlingenMåVæreUnderBeslutningForÅOverta.left()
            UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                if (this.beslutter == null) {
                    return KunneIkkeOvertaMeldekortBehandling.BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta.left()
                }
                if (this.saksbehandler == saksbehandler.navIdent) {
                    return KunneIkkeOvertaMeldekortBehandling.SaksbehandlerOgBeslutterKanIkkeVæreDenSamme.left()
                }
                this.copy(
                    beslutter = saksbehandler.navIdent,
                ).right()
            }
            GODKJENT,
            IKKE_RETT_TIL_TILTAKSPENGER,
            AUTOMATISK_BEHANDLET,
            -> KunneIkkeOvertaMeldekortBehandling.BehandlingenKanIkkeVæreGodkjentEllerIkkeRett.left()
        }
    }

    override fun taMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        return when (this.status) {
            KLAR_TIL_BESLUTNING -> {
                check(saksbehandler.navIdent != this.saksbehandler) {
                    "Beslutter ($saksbehandler) kan ikke være den samme som saksbehandleren (${this.saksbehandler}"
                }
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == null) { "Meldekortbehandlingen har en eksisterende beslutter. For å overta meldekortbehandlingen, bruk overta() - meldekortId: ${this.id}" }
                this.copy(
                    beslutter = saksbehandler.navIdent,
                    status = UNDER_BESLUTNING,
                )
            }

            KLAR_TIL_BEHANDLING,
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
            UNDER_BESLUTNING -> {
                krevBeslutterRolle(saksbehandler)
                require(this.beslutter == saksbehandler.navIdent)
                this.copy(
                    beslutter = null,
                    status = KLAR_TIL_BESLUTNING,
                )
            }

            KLAR_TIL_BEHANDLING,
            KLAR_TIL_BESLUTNING,
            GODKJENT,
            AUTOMATISK_BEHANDLET,
            IKKE_RETT_TIL_TILTAKSPENGER,
            AVBRUTT,
            -> {
                throw IllegalArgumentException(
                    "Kan ikke legge tilbake meldekortbehandling når behandlingen har status ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}",
                )
            }
        }
    }

    fun tilUnderBehandling(
        nyMeldeperiode: Meldeperiode?,
        ikkeRettTilTiltakspengerTidspunkt: LocalDateTime? = null,
    ): MeldekortUnderBehandling {
        require(this.status !in listOf(GODKJENT, IKKE_RETT_TIL_TILTAKSPENGER, AUTOMATISK_BEHANDLET, AVBRUTT)) {
            "Kan ikke gå fra GODKJENT, AUTOMATISK_BEHANDLET, AVBRUTT eller IKKE_RETT_TIL_TILTAKSPENGER til UNDER_BEHANDLING"
        }
        val meldeperiode = nyMeldeperiode ?: this.meldeperiode
        return MeldekortUnderBehandling(
            id = this.id,
            sakId = this.sakId,
            saksnummer = this.saksnummer,
            fnr = this.fnr,
            opprettet = this.opprettet,
            saksbehandler = saksbehandler,
            navkontor = this.navkontor,
            ikkeRettTilTiltakspengerTidspunkt = ikkeRettTilTiltakspengerTidspunkt,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            type = type,
            begrunnelse = this.begrunnelse,
            attesteringer = attesteringer,
            sendtTilBeslutning = iverksattTidspunkt,
            beregning = null,
            simulering = null,
            dager = meldeperiode.tilMeldekortDager(),
        )
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
}
