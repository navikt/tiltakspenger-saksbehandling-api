package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
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
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.IkkeInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
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
    override val saksopplysninger: Saksopplysninger,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val sendtTilBeslutning: LocalDateTime?,
    override val attesteringer: Attesteringer,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val avbrutt: Avbrutt?,
    override val ventestatus: Ventestatus,
    override val venterTil: LocalDateTime?,
    override val resultat: SøknadsbehandlingResultat?,
    override val begrunnelseVilkårsvurdering: Begrunnelse?,
    val søknad: Søknad,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
    override val utbetaling: BehandlingUtbetaling?,
    override val klagebehandling: Klagebehandling?,
) : Rammebehandling {

    override val vedtaksperiode = resultat?.vedtaksperiode
    override val omgjørRammevedtak: OmgjørRammevedtak = resultat?.omgjørRammevedtak ?: OmgjørRammevedtak.empty

    /** Vil være null ved avslag og ved innvilgelse frem til saksbehandler har valgt innvilgelsesperioden */
    override val innvilgelsesperioder = (resultat as? Innvilgelse)?.innvilgelsesperioder

    override val antallDagerPerMeldeperiode by lazy { resultat?.antallDagerPerMeldeperiode }

    override val valgteTiltaksdeltakelser by lazy { resultat?.valgteTiltaksdeltakelser }

    override val barnetillegg = resultat?.barnetillegg

    val kravtidspunkt: LocalDateTime = søknad.tidsstempelHosOss

    /**
     * To kriterier må være oppfylt for at en søknadsbehandling skal kunne innvilges:
     * 1. Det må være søkt på en identifiserbar tiltaksdeltakelse som gir rett til tiltakspenger.
     * 2. Tiltaksdeltakelsen det er søkt på må matches med tiltaksdeltakelseregisteret, ha en tiltaksstype og status som gir rett til tiltakspenger og ha en definert periode.
     */
    val kanInnvilges: Boolean by lazy {
        when (søknad) {
            is InnvilgbarSøknad -> saksopplysninger.kanInnvilges(søknad.tiltak.tiltaksdeltakerId)
            is IkkeInnvilgbarSøknad -> false
        }
    }

    init {
        super.init()

        when (status) {
            KLAR_TIL_BESLUTNING,
            UNDER_BESLUTNING,
            VEDTATT,
            -> require(resultat!!.erFerdigutfylt(saksopplysninger)) {
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
        omgjørRammevedtak: OmgjørRammevedtak,
    ): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
        validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

        val resultat = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Avslag -> {
                Avslag(
                    avslagsgrunner = kommando.avslagsgrunner,
                    avslagsperiode = this.søknad.tiltaksdeltakelseperiodeDetErSøktOm(),
                )
            }

            is OppdaterSøknadsbehandlingKommando.Innvilgelse -> {
                Innvilgelse(
                    barnetillegg = kommando.barnetillegg,
                    innvilgelsesperioder = kommando.tilInnvilgelseperioder(this),
                    omgjørRammevedtak = omgjørRammevedtak,
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
            require(it.resultat?.erFerdigutfylt(saksopplysninger) != false) {
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

    override fun avbryt(
        avbruttAv: Saksbehandler,
        begrunnelse: NonBlankString,
        tidspunkt: LocalDateTime,
        skalAvbryteSøknad: Boolean,
    ): Søknadsbehandling {
        when (status) {
            UNDER_AUTOMATISK_BEHANDLING, KLAR_TIL_BEHANDLING, UNDER_BEHANDLING, KLAR_TIL_BESLUTNING, UNDER_BESLUTNING -> Unit
            VEDTATT, AVBRUTT -> throw IllegalArgumentException("Kan ikke avbryte en søknadsbehandling i tilstanden $status")
        }
        return this.copy(
            status = AVBRUTT,
            søknad = if (skalAvbryteSøknad) this.søknad.avbryt(avbruttAv, begrunnelse, tidspunkt) else this.søknad,
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
            sistEndret = tidspunkt,
        )
    }

    override fun erFerdigutfylt(): Boolean {
        return resultat?.erFerdigutfylt(saksopplysninger) ?: false
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
            søknadsbehandlingId: BehandlingId = BehandlingId.random(),
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: HentSaksopplysninger,
            correlationId: CorrelationId,
            klagebehandling: Klagebehandling?,
            clock: Clock,
        ): Pair<Sak, Søknadsbehandling> {
            val opprettet = nå(clock)

            val saksopplysninger = when (søknad) {
                is InnvilgbarSøknad -> hentSaksopplysninger(
                    sak.fnr,
                    correlationId,
                    sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    listOf(søknad.tiltak.tiltaksdeltakerId),
                    true,
                )

                is IkkeInnvilgbarSøknad -> hentSaksopplysninger(
                    sak.fnr,
                    correlationId,
                    sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    søknad.tiltak?.let { listOf(it.tiltaksdeltakerId) } ?: emptyList(),
                    true,
                )
            }

            return Søknadsbehandling(
                id = søknadsbehandlingId,
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
                klagebehandling = klagebehandling,
            ).let {
                Pair(sak.leggTilSøknadsbehandling(it), it)
            }
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
                sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
                listOf(søknad.tiltak.tiltaksdeltakerId),
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
                klagebehandling = null,
            )
        }
    }
}
