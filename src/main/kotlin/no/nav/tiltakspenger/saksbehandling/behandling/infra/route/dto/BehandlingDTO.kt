package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregninger
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilVentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toTiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import java.time.LocalDateTime

sealed interface BehandlingDTO {
    val id: String
    val type: BehandlingstypeDTO
    val status: BehandlingsstatusDTO
    val resultat: BehandlingResultatDTO?
    val sakId: String
    val saksnummer: String
    val saksbehandler: String?
    val beslutter: String?
    val saksopplysninger: SaksopplysningerDTO?
    val attesteringer: List<AttesteringDTO>
    val virkningsperiode: PeriodeDTO?
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?
    val avbrutt: AvbruttDTO?
    val sistEndret: LocalDateTime
    val iverksattTidspunkt: LocalDateTime?
    val ventestatus: VentestatusHendelseDTO?
    val utbetaling: BehandlingUtbetalingDTO?
}

data class SøknadsbehandlingDTO(
    override val id: String,
    override val status: BehandlingsstatusDTO,
    override val resultat: BehandlingResultatDTO?,
    override val sakId: String,
    override val saksnummer: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val saksopplysninger: SaksopplysningerDTO?,
    override val attesteringer: List<AttesteringDTO>,
    override val virkningsperiode: PeriodeDTO?,
    override val fritekstTilVedtaksbrev: String?,
    override val begrunnelseVilkårsvurdering: String?,
    override val avbrutt: AvbruttDTO?,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val ventestatus: VentestatusHendelseDTO?,
    override val utbetaling: BehandlingUtbetalingDTO?,
    val søknad: SøknadDTO?,
    val barnetillegg: BarnetilleggDTO?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?,
    val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>?,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<String>,
) : BehandlingDTO {
    override val type = BehandlingstypeDTO.SØKNADSBEHANDLING
}

data class RevurderingDTO(
    override val id: String,
    override val status: BehandlingsstatusDTO,
    override val resultat: BehandlingResultatDTO,
    override val sakId: String,
    override val saksnummer: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val saksopplysninger: SaksopplysningerDTO,
    override val attesteringer: List<AttesteringDTO>,
    override val virkningsperiode: PeriodeDTO?,
    override val fritekstTilVedtaksbrev: String?,
    override val begrunnelseVilkårsvurdering: String?,
    override val avbrutt: AvbruttDTO?,
    override val sistEndret: LocalDateTime,
    override val iverksattTidspunkt: LocalDateTime?,
    override val ventestatus: VentestatusHendelseDTO?,
    override val utbetaling: BehandlingUtbetalingDTO?,
    val valgtHjemmelHarIkkeRettighet: List<String>?,
    val barnetillegg: BarnetilleggDTO?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?,
    val harValgtStansFraFørsteDagSomGirRett: Boolean?,
    val harValgtStansTilSisteDagSomGirRett: Boolean?,

) : BehandlingDTO {
    override val type = BehandlingstypeDTO.REVURDERING
}

fun Sak.tilBehandlingDTO(behandlingId: BehandlingId): BehandlingDTO {
    val behandling = rammebehandlinger.hentBehandling(behandlingId)

    requireNotNull(behandling) {
        "Fant ingen behandling med id $behandlingId"
    }

    return when (behandling) {
        is Revurdering -> behandling.tilRevurderingDTO(
            utbetalingsstatus = utbetalinger.hentUtbetalingForBehandlingId(behandlingId)?.status,
            beregninger = meldeperiodeBeregninger,
        )

        is Søknadsbehandling -> behandling.tilSøknadsbehandlingDTO(
            utbetalingsstatus = utbetalinger.hentUtbetalingForBehandlingId(behandlingId)?.status,
            beregninger = meldeperiodeBeregninger,
        )
    }
}

fun Sak.tilBehandlingerDTO(): List<BehandlingDTO> = this.rammebehandlinger.map { this.tilBehandlingDTO(it.id) }

fun Søknadsbehandling.tilSøknadsbehandlingDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    beregninger: MeldeperiodeBeregninger,
): SøknadsbehandlingDTO {
    return SøknadsbehandlingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        resultat = this.resultat?.tilBehandlingResultatDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger?.toSaksopplysningerDTO(),
        søknad = this.søknad.toSøknadDTO(),
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        sistEndret = this.sistEndret,
        iverksattTidspunkt = this.iverksattTidspunkt,
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        virkningsperiode = this.virkningsperiode?.toDTO(),
        antallDagerPerMeldeperiode = this.antallDagerPerMeldeperiode?.toDTO(),
        barnetillegg = null,
        valgteTiltaksdeltakelser = null,
        avslagsgrunner = null,
        automatiskSaksbehandlet = this.automatiskSaksbehandlet,
        manueltBehandlesGrunner = this.manueltBehandlesGrunner.map { it.name },
        ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
        utbetaling = utbetaling?.tilDTO(utbetalingsstatus, beregninger),
    ).let {
        when (resultat) {
            is SøknadsbehandlingResultat.Innvilgelse -> it.copy(
                barnetillegg = resultat.barnetillegg?.toBarnetilleggDTO(),
                valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser.tilDTO(),
            )

            is SøknadsbehandlingResultat.Avslag -> it.copy(
                avslagsgrunner = resultat.avslagsgrunner.toValgtHjemmelForAvslagDTO(),
            )

            null -> it
        }
    }
}

fun Revurdering.tilRevurderingDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    beregninger: MeldeperiodeBeregninger,
): RevurderingDTO {
    return RevurderingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        virkningsperiode = this.virkningsperiode?.toDTO(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        sistEndret = this.sistEndret,
        iverksattTidspunkt = this.iverksattTidspunkt,
        resultat = this.resultat.tilBehandlingResultatDTO(),
        valgtHjemmelHarIkkeRettighet = null,
        valgteTiltaksdeltakelser = null,
        antallDagerPerMeldeperiode = null,
        barnetillegg = null,
        ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
        utbetaling = utbetaling?.tilDTO(utbetalingsstatus, beregninger),
        harValgtStansFraFørsteDagSomGirRett = (this.resultat as? RevurderingResultat.Stans)?.let { this.resultat.harValgtStansFraFørsteDagSomGirRett },
        harValgtStansTilSisteDagSomGirRett = (this.resultat as? RevurderingResultat.Stans)?.let { this.resultat.harValgtStansTilSisteDagSomGirRett },
    ).let {
        when (resultat) {
            is RevurderingResultat.Stans -> it.copy(
                valgtHjemmelHarIkkeRettighet = resultat.valgtHjemmel.toDTO(this.behandlingstype),
            )

            is RevurderingResultat.Innvilgelse -> it.copy(
                antallDagerPerMeldeperiode = resultat.antallDagerPerMeldeperiode?.perioderMedVerdi?.map {
                    AntallDagerPerMeldeperiodeDTO(
                        antallDagerPerMeldeperiode = it.verdi.value,
                        periode = it.periode.toDTO(),
                    )
                },
                barnetillegg = resultat.barnetillegg?.toBarnetilleggDTO(),
                valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser?.tilDTO(),
            )
        }
    }
}

private fun ValgteTiltaksdeltakelser.tilDTO(): List<TiltaksdeltakelsePeriodeDTO> {
    return periodisering.perioderMedVerdi.toList().map { it.toTiltaksdeltakelsePeriodeDTO() }
}
