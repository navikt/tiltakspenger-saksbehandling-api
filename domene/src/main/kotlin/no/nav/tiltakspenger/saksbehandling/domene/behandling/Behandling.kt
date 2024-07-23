package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkårssett
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vurdering

data class LeggTilSaksopplysningRespons(
    val behandling: Behandling,
    val erEndret: Boolean,
)

interface Behandling {
    val id: BehandlingId
    val sakId: SakId
    val ident: String
    val saksnummer: Saksnummer
    val vurderingsperiode: Periode
    val søknader: List<Søknad>
    val saksbehandler: String?
    val beslutter: String?
    val vilkårssett: Vilkårssett
    val status: BehandlingStatus
    val tilstand: BehandlingTilstand

    val saksopplysninger: List<Saksopplysning> get() = vilkårssett.saksopplysninger
    val vilkårsvurderinger: List<Vurdering> get() = vilkårssett.vilkårsvurderinger

    val utfallsperioder: List<Utfallsperiode> get() = vilkårssett.utfallsperioder

    fun leggTilSøknad(søknad: Søknad): Behandling
    fun leggTilSaksopplysning(saksopplysning: Saksopplysning): LeggTilSaksopplysningRespons
    fun taBehandling(saksbehandler: Saksbehandler): Behandling
    fun avbrytBehandling(saksbehandler: Saksbehandler): Behandling
    fun tilBeslutting(saksbehandler: Saksbehandler): Behandling
    fun iverksett(utøvendeBeslutter: Saksbehandler): Behandling
    fun sendTilbake(utøvendeBeslutter: Saksbehandler): Behandling
    fun vilkårsvurder(): Behandling
    fun saksopplysninger(): List<Saksopplysning>
    fun søknad(): Søknad
}
