package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
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

sealed interface RammebehandlingDTO {
    val id: String
    val type: RammebehandlingstypeDTO
    val status: RammebehandlingsstatusDTO
    val resultat: RammebehandlingResultatDTO
    val sakId: String
    val saksnummer: String
    val rammevedtakId: String?
    val saksbehandler: String?
    val beslutter: String?
    val saksopplysninger: SaksopplysningerDTO
    val attesteringer: List<AttesteringDTO>
    val virkningsperiode: PeriodeDTO?
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?
    val avbrutt: AvbruttDTO?
    val sistEndret: LocalDateTime
    val iverksattTidspunkt: LocalDateTime?
    val ventestatus: VentestatusHendelseDTO?
    val utbetaling: BehandlingUtbetalingDTO?
    val barnetillegg: BarnetilleggDTO?
    val innvilgelsesperiode: PeriodeDTO?
}

data class SøknadsbehandlingDTO(
    override val id: String,
    override val status: RammebehandlingsstatusDTO,
    override val resultat: RammebehandlingResultatDTO,
    override val sakId: String,
    override val saksnummer: String,
    override val rammevedtakId: String?,
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
    override val barnetillegg: BarnetilleggDTO?,
    override val innvilgelsesperiode: PeriodeDTO?,
    val søknad: SøknadDTO,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?,
    val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>?,
    val automatiskSaksbehandlet: Boolean,
    val manueltBehandlesGrunner: List<String>,
) : RammebehandlingDTO {
    override val type = RammebehandlingstypeDTO.SØKNADSBEHANDLING
}

data class RevurderingDTO(
    override val id: String,
    override val status: RammebehandlingsstatusDTO,
    override val resultat: RammebehandlingResultatDTO,
    override val sakId: String,
    override val saksnummer: String,
    override val rammevedtakId: String?,
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
    override val barnetillegg: BarnetilleggDTO?,
    override val innvilgelsesperiode: PeriodeDTO?,
    val valgtHjemmelHarIkkeRettighet: List<String>?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?,
    val harValgtStansFraFørsteDagSomGirRett: Boolean?,
    val harValgtStansTilSisteDagSomGirRett: Boolean?,
    val omgjørVedtak: String?,
) : RammebehandlingDTO {
    override val type = RammebehandlingstypeDTO.REVURDERING
}

fun Sak.tilBehandlingDTO(behandlingId: BehandlingId): RammebehandlingDTO {
    val behandling = rammebehandlinger.hentBehandling(behandlingId)

    requireNotNull(behandling) {
        "Fant ingen behandling med id $behandlingId"
    }

    val rammevedtakId = rammevedtaksliste.finnRammevedtakForBehandling(behandlingId)?.id

    return when (behandling) {
        is Revurdering -> behandling.tilRevurderingDTO(
            utbetalingsstatus = utbetalinger.hentUtbetalingForBehandlingId(behandlingId)?.status,
            beregninger = meldeperiodeBeregninger,
            rammevedtakId = rammevedtakId,
        )

        is Søknadsbehandling -> behandling.tilSøknadsbehandlingDTO(
            utbetalingsstatus = utbetalinger.hentUtbetalingForBehandlingId(behandlingId)?.status,
            beregninger = meldeperiodeBeregninger,
            rammevedtakId = rammevedtakId,
        )
    }
}

fun Sak.tilBehandlingerDTO(): List<RammebehandlingDTO> = this.rammebehandlinger.map { this.tilBehandlingDTO(it.id) }

fun Søknadsbehandling.tilSøknadsbehandlingDTO(
    utbetalingsstatus: Utbetalingsstatus?,
    beregninger: MeldeperiodeBeregningerVedtatt,
    rammevedtakId: VedtakId?,
): SøknadsbehandlingDTO {
    return SøknadsbehandlingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        resultat = this.resultat.tilBehandlingResultatDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        rammevedtakId = rammevedtakId.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        søknad = this.søknad.toSøknadDTO(),
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        sistEndret = this.sistEndret,
        iverksattTidspunkt = this.iverksattTidspunkt,
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        virkningsperiode = this.virkningsperiode?.toDTO(),
        antallDagerPerMeldeperiode = null,
        barnetillegg = null,
        valgteTiltaksdeltakelser = null,
        avslagsgrunner = null,
        automatiskSaksbehandlet = this.automatiskSaksbehandlet,
        manueltBehandlesGrunner = this.manueltBehandlesGrunner.map { it.name },
        ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
        utbetaling = utbetaling?.tilDTO(utbetalingsstatus, beregninger),
        innvilgelsesperiode = this.innvilgelsesperiode?.toDTO(),
    ).let {
        when (resultat) {
            is SøknadsbehandlingResultat.Innvilgelse -> it.copy(
                barnetillegg = resultat.barnetillegg?.toBarnetilleggDTO(),
                valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser.tilDTO(),
                antallDagerPerMeldeperiode = this.antallDagerPerMeldeperiode?.toDTO(),
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
    beregninger: MeldeperiodeBeregningerVedtatt,
    rammevedtakId: VedtakId?,
): RevurderingDTO {
    return RevurderingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        rammevedtakId = rammevedtakId.toString(),
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
        omgjørVedtak = null,
        innvilgelsesperiode = this.innvilgelsesperiode?.toDTO(),
        ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
        utbetaling = utbetaling?.tilDTO(utbetalingsstatus, beregninger),
        harValgtStansFraFørsteDagSomGirRett = null,
        harValgtStansTilSisteDagSomGirRett = null,
    ).let {
        when (resultat) {
            is RevurderingResultat.Stans -> it.copy(
                valgtHjemmelHarIkkeRettighet = resultat.valgtHjemmel.toDTO(this.behandlingstype),
                harValgtStansFraFørsteDagSomGirRett = this.resultat.harValgtStansFraFørsteDagSomGirRett,
                harValgtStansTilSisteDagSomGirRett = this.resultat.harValgtStansTilSisteDagSomGirRett,
            )

            is RevurderingResultat.Innvilgelse -> it.copy(
                antallDagerPerMeldeperiode = resultat.antallDagerPerMeldeperiode?.toDTO(),
                barnetillegg = resultat.barnetillegg?.toBarnetilleggDTO(),
                valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser?.tilDTO(),
            )

            is RevurderingResultat.Omgjøring -> it.copy(
                omgjørVedtak = resultat.omgjørRammevedtak.id.toString(),
            )
        }
    }
}

private fun ValgteTiltaksdeltakelser.tilDTO(): List<TiltaksdeltakelsePeriodeDTO> {
    return periodisering.perioderMedVerdi.toList().map { it.toTiltaksdeltakelsePeriodeDTO() }
}
