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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.barnetillegg.OppdaterBarnetilleggCommand
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KunneIkkeOppdatereBarnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.infra.setup.AUTOMATISK_SAKSBEHANDLER_ID
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
    override val resultat: SøknadsbehandlingResultat?,
    override val virkningsperiode: Periode?,
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    val søknad: Søknad,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<ManueltBehandlesGrunn>,
) : Behandling {

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

        when (resultat) {
            is SøknadsbehandlingResultat.Innvilgelse -> resultat.valider(status, virkningsperiode)
            is SøknadsbehandlingResultat.Avslag -> Unit
            null -> Unit
        }
    }

    fun tilBeslutning(
        kommando: OppdaterSøknadsbehandlingKommando,
        clock: Clock,
    ): Either<KanIkkeSendeTilBeslutter, Søknadsbehandling> {
        validerKanSendeTilBeslutning(kommando.saksbehandler).onLeft { return it.left() }

        val status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING

        val (virkningsperiode, resultat) = virkningsperiodeOgResultat(kommando)

        return this.copy(
            status = status,
            sendtTilBeslutning = nå(clock),
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = virkningsperiode,
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            resultat = resultat,
            automatiskSaksbehandlet = kommando.automatiskSaksbehandlet,
        ).right()
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
        ).right()
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

    fun oppdaterBarnetillegg(kommando: OppdaterBarnetilleggCommand): Either<KunneIkkeOppdatereBarnetillegg, Søknadsbehandling> {
        return validerKanOppdatere(kommando.saksbehandler).mapLeft {
            KunneIkkeOppdatereBarnetillegg.KunneIkkeOppdatereBehandling(it)
        }.map {
            this.copy(
                resultat = SøknadsbehandlingResultat.Innvilgelse(
                    valgteTiltaksdeltakelser = kommando.valgteTiltaksdeltakelser(this),
                    barnetillegg = kommando.barnetillegg,
                    antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                ),
            )
        }
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
            is OppdaterSøknadsbehandlingKommando.Avslag -> this.søknad.vurderingsperiode()
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
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: suspend (saksopplysningsperiode: Periode) -> Saksopplysninger,
            clock: Clock,
        ): Either<KanIkkeOppretteBehandling, Søknadsbehandling> {
            krevSaksbehandlerRolle(saksbehandler)
            val opprettet = nå(clock)

            /** Kommentar jah: Det kan bli aktuelt at saksbehandler får endre på fraOgMed her. */
            val saksopplysningsperiode: Periode = søknad.saksopplysningsperiode()

            val saksopplysninger = hentSaksopplysninger(saksopplysningsperiode)

            if (saksopplysninger.tiltaksdeltagelse.isEmpty()) {
                return KanIkkeOppretteBehandling.IngenRelevanteTiltak.left()
            }

            return Søknadsbehandling(
                id = BehandlingId.random(),
                saksnummer = saksnummer,
                sakId = sakId,
                fnr = fnr,
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
                resultat = null,
                virkningsperiode = null,
                begrunnelseVilkårsvurdering = null,
                automatiskSaksbehandlet = false,
                manueltBehandlesGrunner = emptyList(),
            ).right()
        }

        suspend fun opprettAutomatiskBehandling(
            søknad: Søknad,
            hentSaksopplysninger: suspend (saksopplysningsperiode: Periode) -> Saksopplysninger,
            clock: Clock,
        ): Søknadsbehandling {
            val opprettet = nå(clock)
            val saksopplysningsperiode: Periode = søknad.saksopplysningsperiode()
            val saksopplysninger = hentSaksopplysninger(saksopplysningsperiode)

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
                resultat = null,
                virkningsperiode = null,
                begrunnelseVilkårsvurdering = null,
                automatiskSaksbehandlet = false,
                manueltBehandlesGrunner = emptyList(),
            )
        }
    }
}
