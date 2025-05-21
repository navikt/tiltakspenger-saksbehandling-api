package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAttesteringDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.toSøknadDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toTiltaksdeltakelsePeriodeDTO

data class BehandlingDTO(
    val id: String,
    val type: BehandlingstypeDTO,
    val status: BehandlingsstatusDTO,
    val utfall: BehandlingsutfallGammelDTO?,
    val sakId: String,
    val saksnummer: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val saksopplysninger: SaksopplysningerDTO?,
    val attesteringer: List<AttesteringDTO>,
    val søknad: SøknadDTO?,
    val virkningsperiode: PeriodeDTO?,
    val saksopplysningsperiode: PeriodeDTO?,
    val fritekstTilVedtaksbrev: String?,
    val begrunnelseVilkårsvurdering: String?,
    val barnetillegg: BarnetilleggDTO?,
    val avbrutt: AvbruttDTO?,
    val iverksattTidspunkt: String?,
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
    val valgtHjemmelHarIkkeRettighet: List<String>?,
    val antallDagerPerMeldeperiode: Int?,
    val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>?,
)

fun Behandling.toDTO(): BehandlingDTO {
    return when (this) {
        is Revurdering -> this.toDTO()
        is Søknadsbehandling -> this.toDTO()
    }
}

fun Behandlinger.toDTO() = this.map { it.toDTO() }

fun Søknadsbehandling.toDTO(): BehandlingDTO {
    val utenUtfallDTO = BehandlingDTO(
        id = this.id.toString(),
        type = this.behandlingstype.tilBehandlingstypeDTO(),
        status = this.status.toBehandlingsstatusDTO(),
        utfall = this.utfall?.toBehandlingsutfallDto(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        søknad = this.søknad.toSøknadDTO(),
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        saksopplysningsperiode = this.saksopplysningsperiode.toDTO(),
        iverksattTidspunkt = this.iverksattTidspunkt?.toString(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        virkningsperiode = this.virkningsperiode?.toDTO(),
        antallDagerPerMeldeperiode = this.antallDagerPerMeldeperiode,
        barnetillegg = null,
        valgteTiltaksdeltakelser = null,
        valgtHjemmelHarIkkeRettighet = null,
        avslagsgrunner = null,
    )

    val utfall = this.utfall

    return when (utfall) {
        is SøknadsbehandlingUtfall.Innvilgelse -> utenUtfallDTO.copy(
            barnetillegg = utfall.barnetillegg?.toBarnetilleggDTO(),
            valgteTiltaksdeltakelser = utfall.valgteTiltaksdeltakelser?.periodisering?.perioderMedVerdi?.map { it.toTiltaksdeltakelsePeriodeDTO() },
        )
        is SøknadsbehandlingUtfall.Avslag -> utenUtfallDTO.copy(
            avslagsgrunner = utfall.avslagsgrunner.toValgtHjemmelForAvslagDTO(),
        )
        null -> utenUtfallDTO
    }
}

fun Revurdering.toDTO(): BehandlingDTO {
    val utenUtfallDTO = BehandlingDTO(
        id = this.id.toString(),
        type = this.behandlingstype.tilBehandlingstypeDTO(),
        status = this.status.toBehandlingsstatusDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        virkningsperiode = this.virkningsperiode?.toDTO(),
        saksopplysningsperiode = this.saksopplysningsperiode.toDTO(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        iverksattTidspunkt = this.iverksattTidspunkt?.toString(),
        utfall = this.utfall?.toBehandlingsutfallDto(),
        valgtHjemmelHarIkkeRettighet = null,
        valgteTiltaksdeltakelser = null,
        antallDagerPerMeldeperiode = null,
        barnetillegg = null,
        avslagsgrunner = null,
        søknad = null,
    )

    val utfall = this.utfall

    return when (utfall) {
        is RevurderingUtfall.Stans -> utenUtfallDTO.copy(
            valgtHjemmelHarIkkeRettighet = utfall.valgtHjemmelHarIkkeRettighet.toDTO(this.behandlingstype),
        )
        null -> utenUtfallDTO
    }
}
