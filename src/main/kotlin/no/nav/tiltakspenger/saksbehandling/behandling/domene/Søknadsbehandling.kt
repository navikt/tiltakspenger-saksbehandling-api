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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import java.time.Clock
import java.time.LocalDateTime

data class Søknadsbehandling(
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
    override val resultat: SøknadsbehandlingResultat?,
    override val virkningsperiode: Periode?,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    val søknad: Søknad,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
) : Behandling {
    override val utbetaling = null

    override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?
        get() = when (resultat) {
            is SøknadsbehandlingResultat.Avslag -> null
            is SøknadsbehandlingResultat.Innvilgelse -> resultat.antallDagerPerMeldeperiode
            null -> null
        }

    override val barnetillegg: Barnetillegg?
        get() = when (resultat) {
            is SøknadsbehandlingResultat.Avslag -> null
            is SøknadsbehandlingResultat.Innvilgelse -> resultat.barnetillegg
            null -> null
        }

    override val utfallsperioder: SammenhengendePeriodisering<Utfallsperiode>? =
        virkningsperiode?.let { SammenhengendePeriodisering(Utfallsperiode.RETT_TIL_TILTAKSPENGER, it) }

    override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = when (resultat) {
        is SøknadsbehandlingResultat.Avslag -> null
        is SøknadsbehandlingResultat.Innvilgelse -> resultat.valgteTiltaksdeltakelser
        null -> null
    }

    val kravtidspunkt: LocalDateTime = søknad.tidsstempelHosOss

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
            is SøknadsbehandlingResultat.Innvilgelse -> resultat.valider(virkningsperiode)
            is SøknadsbehandlingResultat.Avslag -> Unit
            null -> Unit
        }
    }

    fun oppdater(
        kommando: OppdaterSøknadsbehandlingKommando,
        clock: Clock,
    ): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
        validerKanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

        val (virkningsperiode, resultat) = virkningsperiodeOgResultat(kommando)

        return this.copy(
            sistEndret = nå(clock),
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = virkningsperiode,
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            resultat = resultat,
            automatiskSaksbehandlet = kommando.automatiskSaksbehandlet,
        ).also {
            it.validerResultat()
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
            søknad = this.søknad.avbryt(avbruttAv, begrunnelse, tidspunkt),
            avbrutt = Avbrutt(
                tidspunkt = tidspunkt,
                saksbehandler = avbruttAv.navIdent,
                begrunnelse = begrunnelse,
            ),
        )
    }

    private fun virkningsperiodeOgResultat(kommando: OppdaterSøknadsbehandlingKommando): Pair<Periode, SøknadsbehandlingResultat> {
        val virkningsperiode = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Avslag -> this.søknad.tiltaksdeltagelseperiodeDetErSøktOm()
            is OppdaterSøknadsbehandlingKommando.Innvilgelse -> kommando.innvilgelsesperiode
        }

        val resultat: SøknadsbehandlingResultat = when (kommando) {
            is OppdaterSøknadsbehandlingKommando.Avslag -> {
                SøknadsbehandlingResultat.Avslag(avslagsgrunner = kommando.avslagsgrunner)
            }

            is OppdaterSøknadsbehandlingKommando.Innvilgelse -> {
                SøknadsbehandlingResultat.Innvilgelse(
                    valgteTiltaksdeltakelser = kommando.valgteTiltaksdeltakelser(this),
                    barnetillegg = kommando.barnetillegg,
                    antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                )
            }
        }
        return virkningsperiode to resultat
    }

    companion object {
        suspend fun opprett(
            sak: Sak,
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: HentSaksopplysninger,
            correlationId: CorrelationId,
            clock: Clock,
        ): Either<KanIkkeOppretteBehandling, Søknadsbehandling> {
            val opprettet = nå(clock)

            val saksopplysninger = hentSaksopplysninger(
                sak.fnr,
                correlationId,
                sak.tiltaksdeltagelserDetErSøktTiltakspengerFor,
                listOf(søknad.tiltak.id),
                true,
            )

            if (saksopplysninger.tiltaksdeltagelser.isEmpty()) {
                return KanIkkeOppretteBehandling.IngenRelevanteTiltak.left()
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
                attesteringer = emptyList(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                avbrutt = null,
                ventestatus = Ventestatus(),
                resultat = null,
                virkningsperiode = null,
                begrunnelseVilkårsvurdering = null,
                automatiskSaksbehandlet = false,
                manueltBehandlesGrunner = emptyList(),
            ).right()
        }

        suspend fun opprettAutomatiskBehandling(
            sak: Sak,
            søknad: Søknad,
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
                attesteringer = emptyList(),
                opprettet = opprettet,
                iverksattTidspunkt = null,
                sendtTilDatadeling = null,
                sistEndret = opprettet,
                avbrutt = null,
                ventestatus = Ventestatus(),
                resultat = null,
                virkningsperiode = null,
                begrunnelseVilkårsvurdering = null,
                automatiskSaksbehandlet = false,
                manueltBehandlesGrunner = emptyList(),
            )
        }
    }
}
