package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.LivoppholdSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.LivsoppholdVilkårData
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtak
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vurdering

data class RevurderingVilkårsvurdert(
    override val id: BehandlingId,
    override val sakId: SakId,
    override val vurderingsperiode: Periode,
    override val livsoppholdVilkårData: LivsoppholdVilkårData,
    override val tiltak: List<Tiltak>,
    override val saksbehandler: String?,
    override val forrigeVedtak: Vedtak,
    override val utfallsperioder: Periodisering<Utfallsdetaljer>? = null,
    override val søknader: List<Søknad>,
    val status: BehandlingStatus,
    val vilkårsvurderinger: List<Vurdering>,
) : Revurderingsbehandling {

    fun iverksett(): RevurderingIverksatt {
        return when (status) {
            BehandlingStatus.Manuell -> throw IllegalStateException("Kan ikke iverksette denne behandlingen")
            else ->
                RevurderingIverksatt(
                    id = id,
                    sakId = sakId,
                    forrigeVedtak = forrigeVedtak,
                    vurderingsperiode = vurderingsperiode,
                    livsoppholdVilkårData = livsoppholdVilkårData,
                    tiltak = tiltak,
                    vilkårsvurderinger = vilkårsvurderinger,
                    utfallsperioder = utfallsperioder,
                    saksbehandler = "Automatisk",
                    beslutter = "Automatisk",
                    status = status,
                    søknader = søknader,
                )
        }
    }

    fun tilBeslutting(saksbehandler: Saksbehandler): RevurderingTilBeslutter {
        checkNotNull(this.saksbehandler) { "Ikke lov å sende Behandling til Beslutter uten saksbehandler" }
        check(saksbehandler.navIdent == this.saksbehandler) { "Det er ikke lov å sende en annen sin behandling til beslutter" }

        return when (status) {
            BehandlingStatus.Manuell -> throw IllegalStateException("Kan ikke sende denne behandlingen til beslutter")
            else -> RevurderingTilBeslutter(
                id = id,
                sakId = sakId,
                vurderingsperiode = vurderingsperiode,
                livsoppholdVilkårData = livsoppholdVilkårData,
                tiltak = tiltak,
                vilkårsvurderinger = vilkårsvurderinger,
                utfallsperioder = utfallsperioder,
                saksbehandler = this.saksbehandler,
                beslutter = null,
                status = status,
                forrigeVedtak = forrigeVedtak,
                søknader = søknader,
            )
        }
    }

    override fun leggTilSaksopplysning(livoppholdSaksopplysning: LivoppholdSaksopplysning): LeggTilSaksopplysningRespons =
        spolTilbake().leggTilSaksopplysning(livoppholdSaksopplysning)

    override fun oppdaterTiltak(tiltak: List<Tiltak>): Revurderingsbehandling =
        this.copy(tiltak = tiltak)

    override fun startBehandling(saksbehandler: Saksbehandler): Revurderingsbehandling {
        check(this.saksbehandler == null) { "Denne behandlingen er allerede tatt" }
        check(saksbehandler.isSaksbehandler()) { "Saksbehandler må være saksbehandler" }
        return this.copy(saksbehandler = saksbehandler.navIdent)
    }

    override fun avbrytBehandling(saksbehandler: Saksbehandler): Revurderingsbehandling {
        check(saksbehandler.isSaksbehandler() || saksbehandler.isAdmin()) { "Kan ikke avbryte en behandling som ikke er din" }
        return this.copy(saksbehandler = null)
    }

    private fun spolTilbake(): RevurderingOpprettet =
        RevurderingOpprettet(
            id = id,
            sakId = sakId,
            forrigeVedtak = forrigeVedtak,
            vurderingsperiode = vurderingsperiode,
            livsoppholdVilkårData = livsoppholdVilkårData,
            tiltak = tiltak,
            saksbehandler = saksbehandler,
            søknader = søknader,
        )
}
