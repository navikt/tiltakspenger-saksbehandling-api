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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Omgjøring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class Revurdering(
    override val id: BehandlingId,
    override val status: Rammebehandlingsstatus,
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
    override val attesteringer: Attesteringer,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val avbrutt: Avbrutt?,
    override val ventestatus: Ventestatus,
    override val venterTil: LocalDateTime?,
    override val resultat: RevurderingResultat,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    override val utbetaling: BehandlingUtbetaling?,
) : Rammebehandling {

    override val virkningsperiode: Periode? = resultat.virkningsperiode
    override val innvilgelsesperiode: Periode? = resultat.innvilgelsesperiode

    override val barnetillegg = resultat.barnetillegg

    override val antallDagerPerMeldeperiode = resultat.antallDagerPerMeldeperiode

    override val valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser

    init {
        super.init()

        when (status) {
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            VEDTATT,
            -> require(erFerdigutfylt()) {
                "For tilstandene $KLAR_TIL_BESLUTNING, $UNDER_BESLUTNING og $VEDTATT må resultatet være ferdigutfylt."
            }

            UNDER_AUTOMATISK_BEHANDLING,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            AVBRUTT,
            -> Unit
        }
    }

    /**
     * Sier noe om tilstanden til behandlingen. Er den klar til å sendes til beslutter og/eller iverksettes?
     * Dette er uavhengig av [status], som sier noe om hvor i prosessen behandlingen er.
     */
    fun erFerdigutfylt(): Boolean {
        return when {
            !resultat.erFerdigutfylt(saksopplysninger) -> false
            saksbehandler == null -> false
            else -> true
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
            resultat = Innvilgelse(
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser.periodiser(
                    tiltaksdeltakelser = kommando.tiltaksdeltakelser,
                    behandling = this,
                ),
                barnetillegg = kommando.barnetillegg,
                antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                innvilgelsesperiode = kommando.innvilgelsesperiode,
            ),
            utbetaling = utbetaling,
        ).also {
            // TODO jah: Etter omgjøring, fjern denne sjekken, fjern nullstill resultat og påse at dette gjøres ved send til beslutter + iverksett.
            require(it.resultat.erFerdigutfylt(saksopplysninger))
        }.right()
    }

    fun oppdaterOmgjøring(
        kommando: OppdaterRevurderingKommando.Omgjøring,
        utbetaling: BehandlingUtbetaling?,
        clock: Clock,
    ): Either<KanIkkeOppdatereBehandling, Revurdering> {
        validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

        require(this.resultat is Omgjøring)

        return this.copy(
            sistEndret = nå(clock),
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            resultat = resultat.oppdater(
                innvilgelsesperiode = kommando.innvilgelsesperiode,
                valgteTiltaksdeltakelser = ValgteTiltaksdeltakelser.periodiser(
                    tiltaksdeltakelser = kommando.tiltaksdeltakelser,
                    behandling = this,
                ),
                barnetillegg = kommando.barnetillegg,
                antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
            ),
            utbetaling = utbetaling,
        ).right()
    }

    fun oppdaterStans(
        kommando: OppdaterRevurderingKommando.Stans,
        førsteDagSomGirRett: LocalDate,
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
            resultat = Stans(
                valgtHjemmel = kommando.valgteHjemler,
                harValgtStansFraFørsteDagSomGirRett = kommando.harValgtStansFraFørsteDagSomGirRett,
                harValgtStansTilSisteDagSomGirRett = kommando.harValgtStansTilSisteDagSomGirRett,
                stansperiode = kommando.utledStansperiode(førsteDagSomGirRett, sisteDagSomGirRett),
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
            sistEndret = tidspunkt,
        )
    }

    override fun oppdaterSimulering(nySimulering: Simulering?): Revurdering {
        require(this.erUnderBehandling) { "Forventet at behandlingen var under behandling, men var: ${this.status} for sakId: $sakId og behandlingId: $id" }
        return this.copy(utbetaling = utbetaling!!.oppdaterSimulering(nySimulering))
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
                resultat = Stans.empty,
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
                resultat = Innvilgelse.empty,
            )
        }

        /**
         * @param omgjørRammevedtak Rammevedtaket som erstattes i sin helhet.
         */
        fun opprettOmgjøring(
            saksbehandler: Saksbehandler,
            saksopplysninger: Saksopplysninger,
            omgjørRammevedtak: Rammevedtak,
            clock: Clock,
        ): Revurdering {
            return opprett(
                sakId = omgjørRammevedtak.sakId,
                saksnummer = omgjørRammevedtak.saksnummer,
                fnr = omgjørRammevedtak.fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = saksopplysninger,
                opprettet = nå(clock),
                resultat = Omgjøring(omgjørRammevedtak),
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
                attesteringer = Attesteringer.empty(),
                sendtTilBeslutning = null,
                beslutter = null,
                fritekstTilVedtaksbrev = null,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                avbrutt = null,
                ventestatus = Ventestatus(),
                venterTil = null,
                begrunnelseVilkårsvurdering = null,
                utbetaling = null,
            )
        }
    }
}
