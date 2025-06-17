package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toTiltaksdeltakelsePeriodeDTO

sealed interface BehandlingDTO {
    val id: String
    val type: BehandlingstypeDTO
    val status: BehandlingsstatusDTO
    val resultat: BehandlingResultatDTO?
    val sakId: String
    val saksnummer: String
    val saksbehandler: String?
    val beslutter: String?
    val saksopplysninger: SaksopplysningerDTO
    val attesteringer: List<AttesteringDTO>
    val virkningsperiode: PeriodeDTO?
    val fritekstTilVedtaksbrev: String?
    val begrunnelseVilkårsvurdering: String?
    val avbrutt: AvbruttDTO?
    val iverksattTidspunkt: String?
}

data class SøknadsbehandlingDTO(
    override val id: String,
    override val status: BehandlingsstatusDTO,
    override val resultat: SøknadsbehandlingResultatDTO?,
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
    override val iverksattTidspunkt: String?,
    val søknad: SøknadDTO?,
    val barnetillegg: BarnetilleggDTO?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val antallDagerPerMeldeperiode: Int?,
    val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>?,
) : BehandlingDTO {
    override val type = BehandlingstypeDTO.SØKNADSBEHANDLING
}

data class RevurderingDTO(
    override val id: String,
    override val status: BehandlingsstatusDTO,
    override val resultat: RevurderingResultatDTO,
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
    override val iverksattTidspunkt: String?,
    val valgtHjemmelHarIkkeRettighet: List<String>?,
    val barnetillegg: BarnetilleggDTO?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val antallDagerPerMeldeperiode: Int?,
) : BehandlingDTO {
    override val type = BehandlingstypeDTO.REVURDERING
}

fun Behandling.tilBehandlingDTO(): BehandlingDTO {
    return when (this) {
        is Revurdering -> this.tilRevurderingDTO()
        is Søknadsbehandling -> this.tilSøknadsbehandlingDTO()
    }
}

fun Behandlinger.tilBehandlingerDTO() = this.map { it.tilBehandlingDTO() }

fun Søknadsbehandling.tilSøknadsbehandlingDTO(): SøknadsbehandlingDTO {
    return SøknadsbehandlingDTO(
        id = this.id.toString(),
        status = this.status.toBehandlingsstatusDTO(),
        resultat = this.resultat?.tilUtfallDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        søknad = this.søknad.toSøknadDTO(),
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        iverksattTidspunkt = this.iverksattTidspunkt?.toString(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        virkningsperiode = this.virkningsperiode?.toDTO(),
        antallDagerPerMeldeperiode = this.antallDagerPerMeldeperiode,
        barnetillegg = null,
        valgteTiltaksdeltakelser = null,
        avslagsgrunner = null,
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

fun Revurdering.tilRevurderingDTO(): RevurderingDTO {
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
        iverksattTidspunkt = this.iverksattTidspunkt?.toString(),
        resultat = this.resultat.tilUtfallDTO(),
        valgtHjemmelHarIkkeRettighet = null,
        valgteTiltaksdeltakelser = null,
        antallDagerPerMeldeperiode = null,
        barnetillegg = null,
    ).let {
        when (resultat) {
            is RevurderingResultat.Stans -> it.copy(
                valgtHjemmelHarIkkeRettighet = resultat.valgtHjemmel.toDTO(this.behandlingstype),
            )

            is RevurderingResultat.Innvilgelse -> it.copy(
                antallDagerPerMeldeperiode = resultat.antallDagerPerMeldeperiode,
                barnetillegg = resultat.barnetillegg?.toBarnetilleggDTO(),
                valgteTiltaksdeltakelser = resultat.valgteTiltaksdeltakelser?.tilDTO(),
            )
        }
    }
}

private fun ValgteTiltaksdeltakelser.tilDTO(): List<TiltaksdeltakelsePeriodeDTO> {
    return periodisering.perioderMedVerdi.map { it.toTiltaksdeltakelsePeriodeDTO() }
}
