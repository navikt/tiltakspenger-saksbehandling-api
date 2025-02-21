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
    val type: Behandlingstype,
    val status: BehandlingsstatusDTO,
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
    val stønadsdager: StønadsdagerDTO?,
    val vilkårssett: VilkårssettDTO?,
//    Denne burde være med på revurderinger for client-side validering
//    val førsteLovligeStansdato: LocalDate?,
)

internal fun Behandling.toDTO(): BehandlingDTO {
    return BehandlingDTO(
        id = this.id.toString(),
        type = behandlingstype,
        status = this.status.toDTO(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.toString(),
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        attesteringer = this.attesteringer.toDTO(),
        saksopplysninger = this.saksopplysninger.toDTO(),
        søknad = this.søknad?.toDTO(),
        virkningsperiode = this.virkningsperiode?.toDTO(),
        saksopplysningsperiode = this.saksopplysningsperiode?.toDTO(),
        fritekstTilVedtaksbrev = this.fritekstTilVedtaksbrev?.verdi,
        begrunnelseVilkårsvurdering = this.begrunnelseVilkårsvurdering?.verdi,
        stønadsdager = this.stønadsdager?.toDTO(),
        vilkårssett = this.vilkårssett?.toDTO(),
    )
}
