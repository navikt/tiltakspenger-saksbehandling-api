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
    override val utfall: RevurderingResultat,
    override val virkningsperiode: Periode?,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
) : Behandling {

    override val barnetillegg: Barnetillegg? = when (utfall) {
        is Innvilgelse -> utfall.barnetillegg
        is Stans -> null
    }

    override val antallDagerPerMeldeperiode: Int? = when (utfall) {
        is Innvilgelse -> utfall.antallDagerPerMeldeperiode
        is Stans -> null
    }

    override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = when (utfall) {
        is Innvilgelse -> utfall.valgteTiltaksdeltakelser
        is Stans -> null
    }

    override val utfallsperioder: Periodisering<Utfallsperiode>? by lazy {
        if (virkningsperiode == null) {
            return@lazy null
        }

        when (utfall) {
            is Innvilgelse -> Periodisering(Utfallsperiode.RETT_TIL_TILTAKSPENGER, virkningsperiode)
            is Stans -> Periodisering(Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER, virkningsperiode)
        }
    }

    init {
        super.init()

        when (utfall) {
            is Innvilgelse -> utfall.valider(status, virkningsperiode)
            is Stans -> Unit
        }
    }

    // TODO abn: separat håndtering av stans vil antagelig fjernes på sikt
    fun stansTilBeslutning(
        kommando: RevurderingStansTilBeslutningKommando,
        clock: Clock,
    ): Revurdering {
        validerSendTilBeslutning(kommando.saksbehandler)

        requireNotNull(kommando.sisteDagSomGirRett) {
            "Siste dag som gir rett må være bestemt før stans kan sendes til beslutning"
        }

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(clock),
            begrunnelseVilkårsvurdering = kommando.begrunnelse,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = Periode(kommando.stansFraOgMed, kommando.sisteDagSomGirRett),
            utfall = Stans(
                valgtHjemmel = kommando.valgteHjemler,
            ),
        )
    }

    fun tilBeslutning(
        kommando: RevurderingInnvilgelseTilBeslutningKommando,
        clock: Clock,
    ): Revurdering {
        validerSendTilBeslutning(kommando.saksbehandler)

        require(this.utfall is Innvilgelse)

        return this.copy(
            status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING,
            sendtTilBeslutning = nå(clock),
            begrunnelseVilkårsvurdering = kommando.begrunnelse,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = kommando.innvilgelsesperiode,
            utfall = this.utfall.copy(
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser.periodiser(
                    tiltaksdeltakelser = kommando.tiltaksdeltakelser,
                    behandling = this,
                ),
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
                utfall = Stans(
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
            forrigeBehandling: Behandling,
            clock: Clock,
        ): Revurdering {
            val forrigeUtfall = forrigeBehandling.utfall

            require(forrigeUtfall is BehandlingResultat.Innvilgelse)

            return opprett(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = nå(clock),
                utfall = Innvilgelse(
                    valgteTiltaksdeltakelser = forrigeUtfall.valgteTiltaksdeltakelser,
                    barnetillegg = forrigeUtfall.barnetillegg,
                    antallDagerPerMeldeperiode = forrigeUtfall.antallDagerPerMeldeperiode,
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
            utfall: RevurderingResultat,
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
                utfall = utfall,
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
