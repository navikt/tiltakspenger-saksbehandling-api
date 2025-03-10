package no.nav.tiltakspenger.vedtak.routes.behandling.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandlingstype

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
    val barnetillegg: BarnetilleggDTO?,
    val avbrutt: AvbruttDTO?,
    val iverksattTidspunkt: String?,
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
        barnetillegg = this.barnetillegg?.toDTO(),
        avbrutt = this.avbrutt?.toDTO(),
        iverksattTidspunkt = this.iverksattTidspunkt?.toString(),
    )
}

internal fun Behandlinger.toDTO() = this.map { it.toDTO() }
