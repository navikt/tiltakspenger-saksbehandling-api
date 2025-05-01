package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
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

internal data class BehandlingDTO(
    val id: String,
    val type: Behandlingstype,
    val status: BehandlingsstatusDTO,
    val utfall: BehandlingsutfallDTO,
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
    val avslagsgrunner: List<String>,
)

internal fun Behandling.toDTO(): BehandlingDTO {
    return BehandlingDTO(
        id = this.id.toString(),
        type = behandlingstype,
        status = this.status.toBehandlingsstatusDTO(),
        utfall = this.utfall.toBehandlingsutfallDto(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toAttesteringDTO(),
        saksopplysninger = this.saksopplysninger.toSaksopplysningerDTO(),
        søknad = this.søknad?.toSøknadDTO(),
        virkningsperiode = this.virkningsperiode?.toDTO(),
        saksopplysningsperiode = this.saksopplysningsperiode?.toDTO(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        barnetillegg = this.barnetillegg?.toBarnetilleggDTO(),
        avbrutt = this.avbrutt?.toAvbruttDTO(),
        iverksattTidspunkt = this.iverksattTidspunkt?.toString(),
        valgteTiltaksdeltakelser = this.valgteTiltaksdeltakelser?.periodisering?.perioderMedVerdi?.map { it.toTiltaksdeltakelsePeriodeDTO() },
        valgtHjemmelHarIkkeRettighet = this.valgtHjemmelHarIkkeRettighet.toDTO(this.behandlingstype),
        antallDagerPerMeldeperiode = this.antallDagerPerMeldeperiode,
        avslagsgrunner = this.avslagsgrunner.toList().toDTO(behandlingstype),
    )
}

internal fun Behandlinger.toDTO() = this.map { it.toDTO() }
