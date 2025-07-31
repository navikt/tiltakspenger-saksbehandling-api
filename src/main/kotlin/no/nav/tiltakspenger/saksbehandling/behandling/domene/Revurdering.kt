package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import java.time.Clock
import java.time.LocalDateTime

data class Revurdering(
    override val id: BehandlingId,
    override val status: Behandlingsstatus,
    override val opprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val sendtTilDatadeling: LocalDateTime?,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val saksopplysninger: Saksopplysninger,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val sendtTilBeslutning: LocalDateTime?,
    override val attesteringer: List<Attestering>,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val avbrutt: Avbrutt?,
    override val resultat: RevurderingResultat,
    override val virkningsperiode: Periode?,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
) : Behandling {

    override val barnetillegg: Barnetillegg? = when (resultat) {
        is Innvilgelse -> resultat.barnetillegg
        is Stans -> null
    }

    override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>? =
        when (resultat) {
            is Innvilgelse -> resultat.antallDagerPerMeldeperiode
            is Stans -> null
        }

    override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = when (resultat) {
        is Innvilgelse -> resultat.valgteTiltaksdeltakelser
        is Stans -> null
    }

    override val utfallsperioder: SammenhengendePeriodisering<Utfallsperiode>? by lazy {
        if (virkningsperiode == null) {
            return@lazy null
        }

        when (resultat) {
            is Innvilgelse -> SammenhengendePeriodisering(Utfallsperiode.RETT_TIL_TILTAKSPENGER, virkningsperiode)
            is Stans -> SammenhengendePeriodisering(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, virkningsperiode)
        }
    }

    val utbetaling: Innvilgelse.Utbetaling? by lazy {
        when (resultat) {
            is Innvilgelse -> resultat.utbetaling
            is Stans -> null
        }
    }

    init {
        super.init()

        when (resultat) {
            is Innvilgelse -> resultat.valider(status, virkningsperiode)
            is Stans -> resultat.valider(status)
        }
    }

    fun stansTilBeslutning(
        kommando: RevurderingTilBeslutningKommando.Stans,
        clock: Clock,
    ): Either<KanIkkeSendeTilBeslutter, Revurdering> {
        return validerSendTilBeslutning(kommando.saksbehandler).mapLeft {
            it
        }.map {
            requireNotNull(kommando.sisteDagSomGirRett) {
                "Siste dag som gir rett må være bestemt før stans kan sendes til beslutning"
            }

            require(resultat is Stans)

            return this.copy(
                status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
                sendtTilBeslutning = nå(clock),
                begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
                virkningsperiode = Periode(kommando.stansFraOgMed, kommando.sisteDagSomGirRett),
                resultat = Stans(
                    valgtHjemmel = kommando.valgteHjemler,
                ),
            ).right()
        }
    }

    fun tilBeslutning(
        kommando: RevurderingTilBeslutningKommando.Innvilgelse,
        utbetaling: Innvilgelse.Utbetaling?,
        clock: Clock,
    ): Either<KanIkkeSendeTilBeslutter, Revurdering> {
        return validerSendTilBeslutning(kommando.saksbehandler).mapLeft {
            it
        }.map {
            require(this.resultat is Innvilgelse)

            this.copy(
                status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
                sendtTilBeslutning = nå(clock),
                begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
                virkningsperiode = kommando.innvilgelsesperiode,
                resultat = this.resultat.copy(
                    valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser.periodiser(
                        tiltaksdeltakelser = kommando.tiltaksdeltakelser,
                        behandling = this,
                    ),
                    barnetillegg = kommando.barnetillegg,
                    antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                    utbetaling = utbetaling,
                ),
            )
        }
    }

    override fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Revurdering {
        if (this.status == AVBRUTT || avbrutt != null) {
            throw IllegalArgumentException("Behandlingen er allerede avbrutt")
        }

        return this.copy(
            status = AVBRUTT,
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
        )
    }

    private fun validerSendTilBeslutning(saksbehandler: Saksbehandler): Either<KanIkkeSendeTilBeslutter, Unit> {
        if (status != UNDER_BEHANDLING) {
            return KanIkkeSendeTilBeslutter.MåVæreUnderBehandlingEllerAutomatisk.left()
        }
        if (saksbehandler.navIdent != this.saksbehandler) {
            return KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(this.saksbehandler).left()
        }
        return Unit.right()
    }

    companion object {
        fun opprettStans(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler,
            saksopplysninger: Saksopplysninger,
            clock: Clock,
        ): Revurdering {
            return opprett(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = nå(clock),
                resultat = Stans(
                    valgtHjemmel = emptyList(),
                ),
            )
        }

        fun opprettInnvilgelse(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler,
            saksopplysninger: Saksopplysninger,
            clock: Clock,
        ): Revurdering {
            return opprett(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = nå(clock),
                resultat = Innvilgelse(
                    valgteTiltaksdeltakelser = null,
                    barnetillegg = null,
                    // TODO John + Anders: Siden vi ikke har en virkningsperiode på dette tidspunktet, gir det ikke noen mening og sette antallDagerPerMeldeperiode
                    antallDagerPerMeldeperiode = null,
                    utbetaling = null,
                ),
            )
        }

        private fun opprett(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            saksbehandler: Saksbehandler,
            saksopplysninger: Saksopplysninger,
            opprettet: LocalDateTime,
            resultat: RevurderingResultat,
        ): Revurdering {
            return Revurdering(
                id = BehandlingId.random(),
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                status = UNDER_BEHANDLING,
                saksbehandler = saksbehandler.navIdent,
                saksopplysninger = saksopplysninger,
                opprettet = opprettet,
                sistEndret = opprettet,
                resultat = resultat,
                attesteringer = emptyList(),
                virkningsperiode = null,
                sendtTilBeslutning = null,
                beslutter = null,
                fritekstTilVedtaksbrev = null,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                avbrutt = null,
                begrunnelseVilkårsvurdering = null,
            )
        }
    }
}
