package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat.Avslag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDateTime

data class Søknadsbehandling(
    override val id: BehandlingId,
    override val status: Rammebehandlingsstatus,
    override val opprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val sendtTilDatadeling: LocalDateTime?,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val saksopplysninger: Saksopplysninger?,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val sendtTilBeslutning: LocalDateTime?,
    override val attesteringer: Attesteringer,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val avbrutt: Avbrutt?,
    override val ventestatus: Ventestatus,
    override val venterTil: LocalDateTime?,
    override val resultat: SøknadsbehandlingResultat?,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    val søknad: Søknad,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
    override val utbetaling: BehandlingUtbetaling?,
) : Rammebehandling {

    override val virkningsperiode = resultat?.virkningsperiode

    override val antallDagerPerMeldeperiode = resultat?.antallDagerPerMeldeperiode

    override val barnetillegg = resultat?.barnetillegg

    override val valgteTiltaksdeltakelser = resultat?.valgteTiltaksdeltakelser

    val kravtidspunkt: LocalDateTime = søknad.tidsstempelHosOss

    init {
        super.init()

        when (status) {
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            VEDTATT,
            -> require(resultat!!.erFerdigutfylt) {
                "Behandlingsresultatet må være ferdigutfylt når status er $status"
            }

            UNDER_AUTOMATISK_BEHANDLING,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            AVBRUTT,
            -> Unit
        }
    }

    /**
     * @param utbetaling null dersom avslag eller dersom behandlingen ikke fører til en beregning.
     */
    fun oppdater(
        kommando: OppdaterSøknadsbehandlingKommando,
        clock: Clock,
        utbetaling: BehandlingUtbetaling?,
    ): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
        validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

        val resultat = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Avslag -> {
                Avslag(avslagsgrunner = kommando.avslagsgrunner, avslagsperiode = this.søknad.tiltaksdeltagelseperiodeDetErSøktOm())
            }

            is OppdaterSøknadsbehandlingKommando.Innvilgelse -> {
                Innvilgelse(
                    valgteTiltaksdeltakelser = kommando.valgteTiltaksdeltakelser(this),
                    barnetillegg = kommando.barnetillegg,
                    antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                    innvilgelsesperiode = kommando.innvilgelsesperiode,
                )
            }

            is OppdaterSøknadsbehandlingKommando.IkkeValgtResultat -> null
        }

        return this.copy(
            sistEndret = nå(clock),
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            resultat = resultat,
            automatiskSaksbehandlet = kommando.automatiskSaksbehandlet,
            utbetaling = utbetaling,
        ).also {
            require(it.resultat?.erFerdigutfylt != false) {
                "Behandlingsresultatet må være ferdigutfylt etter vi oppdaterer søknadsbehandlingen"
            }
        }.right()
    }

    fun tilManuellBehandling(
        manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
        clock: Clock,
    ): Søknadsbehandling {
        check(status == UNDER_AUTOMATISK_BEHANDLING) {
            "Behandlingen må være under automatisk behandling. Behandlingsstatus: ${this.status}."
        }
        return this.copy(
            status = KLAR_TIL_BEHANDLING,
            sistEndret = nå(clock),
            saksbehandler = null,
            manueltBehandlesGrunner = manueltBehandlesGrunner,
        )
    }

    override fun avbryt(avbruttAv: Saksbehandler, begrunnelse: String, tidspunkt: LocalDateTime): Søknadsbehandling {
        if (this.status == AVBRUTT || avbrutt != null) {
            throw IllegalArgumentException("Behandlingen er allerede avbrutt")
        }

        return this.copy(
            status = AVBRUTT,
            søknad = this.søknad.avbryt(avbruttAv, begrunnelse, tidspunkt) as InnvilgbarSøknad,
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
        )
    }

    override fun oppdaterSimulering(nySimulering: Simulering?): Søknadsbehandling {
        require(this.erUnderBehandling) { "Forventet at behandlingen var under behandling, men var: ${this.status} for sakId: $sakId og behandlingId: $id" }
        return this.copy(utbetaling = utbetaling!!.oppdaterSimulering(nySimulering))
    }

    fun oppdaterVenterTil(
        nyVenterTil: LocalDateTime,
        clock: Clock,
    ): Søknadsbehandling {
        require(status == UNDER_AUTOMATISK_BEHANDLING && saksbehandler == AUTOMATISK_SAKSBEHANDLER_ID) {
            "Kun behandlinger under automatisk behandling kan oppdatere venterTil-tidspunkt"
        }
        require(ventestatus.erSattPåVent && venterTil != null) {
            "Kan ikke oppdatere venterTil hvis behandlingen ikke allerede er satt på vent"
        }
        return this.copy(
            venterTil = nyVenterTil,
            sistEndret = nå(clock),
        )
    }

    companion object {
        suspend fun opprett(
            sak: Sak,
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: HentSaksopplysninger,
            correlationId: CorrelationId,
            clock: Clock,
        ): Søknadsbehandling {
            val opprettet = nå(clock)

            val saksopplysninger = if (søknad.tiltak != null) {
                hentSaksopplysninger(
                    sak.fnr,
                    correlationId,
                    sak.tiltaksdeltagelserDetErSøktTiltakspengerFor,
                    listOf(søknad.tiltak!!.id),
                    true,
                )
            } else {
                null
            }

            return Søknadsbehandling(
                id = BehandlingId.random(),
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                fnr = sak.fnr,
                søknad = søknad,
                saksopplysninger = saksopplysninger,
                fritekstTilVedtaksbrev = null,
                saksbehandler = saksbehandler.navIdent,
                sendtTilBeslutning = null,
                beslutter = null,
                status = UNDER_BEHANDLING,
                attesteringer = Attesteringer.empty(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                avbrutt = null,
                ventestatus = Ventestatus(),
                venterTil = null,
                resultat = null,
                begrunnelseVilkårsvurdering = null,
                automatiskSaksbehandlet = false,
                manueltBehandlesGrunner = emptyList(),
                utbetaling = null,
            )
        }

        suspend fun opprettAutomatiskBehandling(
            sak: Sak,
            søknad: InnvilgbarSøknad,
            hentSaksopplysninger: HentSaksopplysninger,
            correlationId: CorrelationId,
            clock: Clock,
        ): Søknadsbehandling {
            val opprettet = nå(clock)
            val saksopplysninger = hentSaksopplysninger(
                søknad.fnr,
                correlationId,
                sak.tiltaksdeltagelserDetErSøktTiltakspengerFor,
                listOf(søknad.tiltak.id),
                true,
            )
            return Søknadsbehandling(
                id = BehandlingId.random(),
                saksnummer = søknad.saksnummer,
                sakId = søknad.sakId,
                fnr = søknad.fnr,
                søknad = søknad,
                saksopplysninger = saksopplysninger,
                fritekstTilVedtaksbrev = null,
                saksbehandler = AUTOMATISK_SAKSBEHANDLER_ID,
                sendtTilBeslutning = null,
                beslutter = null,
                status = UNDER_AUTOMATISK_BEHANDLING,
                attesteringer = Attesteringer.empty(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                avbrutt = null,
                ventestatus = Ventestatus(),
                venterTil = null,
                resultat = null,
                begrunnelseVilkårsvurdering = null,
                automatiskSaksbehandlet = false,
                manueltBehandlesGrunner = emptyList(),
                utbetaling = null,
            )
        }
    }
}
