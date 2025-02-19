package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingstype
import no.nav.tiltakspenger.vedtak.routes.behandling.stønadsdager.StønadsdagerDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.stønadsdager.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.VilkårssettDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.toDTO

internal data class BehandlingDTO(
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val status: BehandlingsstatusDTO,
    val saksbehandler: String?,
    val beslutter: String?,
    val behandlingstype: Behandlingstype,
    val vurderingsperiode: PeriodeDTO,
    val vilkårssett: VilkårssettDTO?,
    val saksopplysninger: SaksopplysningerDTO?,
    val stønadsdager: StønadsdagerDTO?,
    val attesteringer: List<AttesteringDTO>,
    val søknad: SøknadDTO?,
    val fritekstTilVedtaksbrev: String?,
    val begrunnelseVilkårsvurdering: String?,
    val innvilgelsesperiode: PeriodeDTO?,
)

internal fun Behandling.toDTO(): BehandlingDTO {
    return BehandlingDTO(
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        status = this.status.toDTO(),
        vurderingsperiode = this.vurderingsperiode.toDTO(),
        attesteringer = this.attesteringer.toDTO(),
        vilkårssett = this.vilkårssett?.toDTO(),
        saksopplysninger = this.saksopplysninger?.toDTO(),
        stønadsdager = this.stønadsdager?.toDTO(),
        behandlingstype = behandlingstype,
        søknad = this.søknad?.toDTO(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        innvilgelsesperiode = this.innvilgelsesperiode?.toDTO(),
    )
}
