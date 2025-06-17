package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Stans
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
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
    override val oppgaveId: OppgaveId?,
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

    override val antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode>? = when (resultat) {
        is Innvilgelse -> resultat.antallDagerPerMeldeperiode
        is Stans -> null
    }

    override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = when (resultat) {
        is Innvilgelse -> resultat.valgteTiltaksdeltakelser
        is Stans -> null
    }

    override val utfallsperioder: Periodisering<Utfallsperiode>? by lazy {
        if (virkningsperiode == null) {
            return@lazy null
        }

        when (resultat) {
            is Innvilgelse -> Periodisering(Utfallsperiode.RETT_TIL_TILTAKSPENGER, virkningsperiode)
            is Stans -> Periodisering(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, virkningsperiode)
        }
    }

    init {
        super.init()

        when (resultat) {
            is Innvilgelse -> resultat.valider(status, virkningsperiode)
            is Stans -> Unit
        }
    }

    fun stansTilBeslutning(
        kommando: RevurderingStansTilBeslutningKommando,
        clock: Clock,
    ): Revurdering {
        validerSendTilBeslutning(kommando.saksbehandler)

        requireNotNull(kommando.sisteDagSomGirRett) {
            "Siste dag som gir rett må være bestemt før stans kan sendes til beslutning"
        }

        require(resultat is Stans)

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(clock),
            begrunnelseVilkårsvurdering = kommando.begrunnelse,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = Periode(kommando.stansFraOgMed, kommando.sisteDagSomGirRett),
            resultat = Stans(
                valgtHjemmel = kommando.valgteHjemler,
            ),
        )
    }

    fun tilBeslutning(
        kommando: RevurderingInnvilgelseTilBeslutningKommando,
        clock: Clock,
    ): Revurdering {
        validerSendTilBeslutning(kommando.saksbehandler)

        require(this.resultat is Innvilgelse)

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(clock),
            begrunnelseVilkårsvurdering = kommando.begrunnelse,
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
        )
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

    private fun validerSendTilBeslutning(saksbehandler: Saksbehandler) {
        check(status == UNDER_BEHANDLING) {
            "Behandlingen må være under behandling, det innebærer også at en saksbehandler må ta saken før den kan sendes til beslutter. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}"
        }
        check(saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }
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
                oppgaveId = null,
                avbrutt = null,
                begrunnelseVilkårsvurdering = null,
            )
        }
    }
}
