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
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
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
    override val oppgaveId: OppgaveId?,
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
    override val begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering?,
    override val antallDagerPerMeldeperiode: Int?,
    val søknad: Søknad,
) : Behandling {

    override val barnetillegg: Barnetillegg?
        get() = when (utfall) {
            is SøknadsbehandlingUtfall.Avslag -> null
            is SøknadsbehandlingUtfall.Innvilgelse -> utfall.barnetillegg
            null -> null
        }

    override val utfallsperioder: Periodisering<Utfallsperiode>? =
        virkningsperiode?.let { Periodisering(Utfallsperiode.RETT_TIL_TILTAKSPENGER, it) }

    val kravtidspunkt: LocalDateTime = søknad.tidsstempelHosOss

    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = when (utfall) {
        is SøknadsbehandlingUtfall.Avslag -> null
        is SøknadsbehandlingUtfall.Innvilgelse -> utfall.valgteTiltaksdeltakelser
        null -> null
    }

    init {
        super.init()

        when (utfall) {
            is SøknadsbehandlingUtfall.Innvilgelse -> utfall.valider(status, virkningsperiode)
            is SøknadsbehandlingUtfall.Avslag -> Unit
            null -> Unit
        }
    }

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
            SøknadsbehandlingUtfallType.INNVILGELSE -> {
                require(kommando.avslagsgrunner == null) {
                    "Avslagsgrunner kan ikke være satt dersom behandlingen har utfallet INNVILGELSE"
                }

                SøknadsbehandlingUtfall.Innvilgelse(
                    valgteTiltaksdeltakelser = kommando.valgteTiltaksdeltakelser(this),
                    barnetillegg = kommando.barnetillegg,
                )
            }

            SøknadsbehandlingUtfallType.AVSLAG -> {
                requireNotNull(kommando.avslagsgrunner) {
                    "Avslagsgrunner må være satt dersom behandlingen har utfallet AVSLAG"
                }

                SøknadsbehandlingUtfall.Avslag(
                    avslagsgrunner = kommando.avslagsgrunner,
                )
            }
        }

        return this.copy(
            status = status,
            sendtTilBeslutning = nå(clock),
            fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
            virkningsperiode = virkningsperiode,
            begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
            utfall = utfall,
            antallDagerPerMeldeperiode = kommando.antallDagerPerMeldeperiode,
        )
    }

    fun oppdaterBarnetillegg(kommando: OppdaterBarnetilleggKommando): Søknadsbehandling {
        require(this.utfall is SøknadsbehandlingUtfall.Innvilgelse)
        validerKanOppdatere(kommando.saksbehandler, "Kunne ikke oppdatere barnetillegg")

        return this.copy(utfall = utfall.copy(barnetillegg = kommando.barnetillegg(this.virkningsperiode)))
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
                oppgaveId = søknad.oppgaveId,
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
                begrunnelseVilkårsvurdering = null,
                antallDagerPerMeldeperiode = null,
            ).right()
        }
    }
}
