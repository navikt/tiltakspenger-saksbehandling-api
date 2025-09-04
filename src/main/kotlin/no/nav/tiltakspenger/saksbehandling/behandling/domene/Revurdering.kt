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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import java.time.Clock
import java.time.LocalDate
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
    override val ventestatus: Ventestatus,
    override val resultat: RevurderingResultat,
    override val virkningsperiode: Periode?,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    override val utbetaling: BehandlingUtbetaling?,
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

    init {
        super.init()

        when (status) {
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            VEDTATT,
            -> validerResultat()

            UNDER_AUTOMATISK_BEHANDLING,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            AVBRUTT,
            -> Unit
        }
    }

    private fun validerResultat() {
        when (resultat) {
            is Innvilgelse -> resultat.valider(virkningsperiode)
            is Stans -> resultat.valider()
        }
    }

    fun oppdaterInnvilgelse(
        kommando: OppdaterRevurderingKommando.Innvilgelse,
        utbetaling: BehandlingUtbetaling?,
        clock: Clock,
    ): Either<KanIkkeOppdatereBehandling, Revurdering> {
        validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

        require(this.resultat is Innvilgelse)

        return this.copy(
            sistEndret = nå(clock),
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
            ),
            utbetaling = utbetaling,
        ).also {
            it.validerResultat()
        }.right()
    }

    fun oppdaterStans(
        kommando: OppdaterRevurderingKommando.Stans,
        sisteDagSomGirRett: LocalDate,
        utbetaling: BehandlingUtbetaling?,
        clock: Clock,
    ): Either<KanIkkeOppdatereBehandling, Revurdering> {
        validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

        require(this.resultat is Stans)

        return this.copy(
            sistEndret = nå(clock),
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = Periode(kommando.stansFraOgMed, sisteDagSomGirRett),
            resultat = Stans(
                valgtHjemmel = kommando.valgteHjemler,
            ),
            utbetaling = utbetaling,
        ).right()
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
                ventestatus = Ventestatus(),
                begrunnelseVilkårsvurdering = null,
                utbetaling = null,
            )
        }
    }
}
