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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
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
    override val saksopplysningsperiode: Periode,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val sendtTilBeslutning: LocalDateTime?,
    override val attesteringer: List<Attestering>,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val avbrutt: Avbrutt?,
    override val utfall: SøknadsbehandlingUtfall?,
    override val virkningsperiode: Periode?,
    val søknad: Søknad,
) : BehandlingNy() {
    val oppgaveId: OppgaveId? = søknad.oppgaveId
    val kravtidspunkt: LocalDateTime = søknad.tidsstempelHosOss
    val maksDagerMedTiltakspengerForPeriode: Int = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE

    fun tilBeslutning(
        kommando: SendSøknadsbehandlingTilBeslutningKommando,
        clock: Clock,
    ): Søknadsbehandling {
        check(status == UNDER_BEHANDLING) {
            "Behandlingen må være under behandling, det innebærer også at en saksbehandler må ta saken før den kan sendes til beslutter. Behandlingsstatus: ${this.status}. Utøvende saksbehandler: $saksbehandler. Saksbehandler på behandling: ${this.saksbehandler}"
        }
        check(kommando.saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }

        val status = if (beslutter == null) KLAR_TIL_BESLUTNING else UNDER_BESLUTNING
        val virkningsperiode = kommando.behandlingsperiode

        val utfall: SøknadsbehandlingUtfall = when (kommando.utfall) {
            Behandlingsutfall.INNVILGELSE -> SøknadsbehandlingUtfall.Innvilgelse(
                status = status,
                virkningsperiode = virkningsperiode,
                antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
                begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
                valgteTiltaksdeltakelser = kommando.valgteTiltaksdeltakelser(this),
                barnetillegg = kommando.barnetillegg,
            )

            Behandlingsutfall.AVSLAG -> SøknadsbehandlingUtfall.Avslag(
                avslagsgrunner = kommando.avslagsgrunner!!,
            )

            Behandlingsutfall.STANS -> throw IllegalArgumentException("Støtter ikke stans her (bør fjerne denne statusen fra kommandoen)")
        }

        return this.copy(
            status = status,
            sendtTilBeslutning = nå(clock),
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = virkningsperiode,
            utfall = utfall,
        )
    }

    fun oppdaterBegrunnelseVilkårsvurdering(
        saksbehandler: Saksbehandler,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
    ): Søknadsbehandling {
        if (!saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("Kunne ikke oppdatere begrunnelse/vilkårsvurdering. Saksbehandler mangler rollen SAKSBEHANDLER. sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != saksbehandler.navIdent) {
            throw IllegalArgumentException("Kunne ikke oppdatere begrunnelse/vilkårsvurdering. Saksbehandler er ikke satt på behandlingen. sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalArgumentException("Kunne ikke oppdatere begrunnelse/vilkårsvurdering. Behandling er ikke under behandling. sakId=$sakId, behandlingId=$id, status=$status")
        }
        require(this.utfall is SøknadsbehandlingUtfall.Innvilgelse)

        return this.copy(utfall = utfall.copy(begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering))
    }

    fun oppdaterBarnetillegg(kommando: OppdaterBarnetilleggKommando): Søknadsbehandling {
        if (!kommando.saksbehandler.erSaksbehandler()) {
            throw IllegalArgumentException("Kunne ikke oppdatere barnetillegg. Saksbehandler mangler rollen SAKSBEHANDLER. sakId=$sakId, behandlingId=$id")
        }
        if (this.saksbehandler != kommando.saksbehandler.navIdent) {
            throw IllegalArgumentException("Kunne ikke oppdatere barnetillegg. Saksbehandler er ikke satt på behandlingen. sakId=$sakId, behandlingId=$id")
        }
        if (!this.erUnderBehandling) {
            throw IllegalArgumentException("Kunne ikke oppdatere barnetillegg. Behandling er ikke under behandling. sakId=$sakId, behandlingId=$id, status=$status")
        }
        require(this.utfall is SøknadsbehandlingUtfall.Innvilgelse)

        return this.copy(utfall = utfall.copy(barnetillegg = kommando.barnetillegg(this.virkningsperiode)))
    }

    companion object {
        /** Hardkoder denne til 10 for nå. På sikt vil vi la saksbehandler periodisere dette selv, litt på samme måte som barnetillegg. */
        const val MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE: Int = 10

        suspend fun opprett(
            sakId: SakId,
            saksnummer: Saksnummer,
            fnr: Fnr,
            søknad: Søknad,
            saksbehandler: Saksbehandler,
            hentSaksopplysninger: suspend (saksopplysningsperiode: Periode) -> Saksopplysninger,
            clock: Clock,
        ): Either<KanIkkeOppretteBehandling, Søknadsbehandling> {
            val opprettet = nå(clock)

            /** Kommentar jah: Det kan bli aktuelt at saksbehandler får endre på fraOgMed her. */
            val saksopplysningsperiode: Periode = run {
                // § 11: Tiltakspenger og barnetillegg gis for opptil tre måneder før den måneden da kravet om ytelsen ble satt fram, dersom vilkårene var oppfylt i denne perioden.
                val fraOgMed = søknad.kravdato.withDayOfMonth(1).minusMonths(3)
                // Forskriften gir ingen begrensninger fram i tid. 100 år bør være nok.
                val tilOgMed = fraOgMed.plusYears(100)
                Periode(fraOgMed, tilOgMed)
            }

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
                saksopplysningsperiode = saksopplysningsperiode,
                avbrutt = null,
                utfall = null,
                virkningsperiode = null,
            ).right()
        }
    }
}
