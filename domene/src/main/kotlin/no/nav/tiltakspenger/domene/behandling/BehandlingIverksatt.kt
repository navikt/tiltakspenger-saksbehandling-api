package no.nav.tiltakspenger.domene.behandling

import no.nav.tiltakspenger.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vilkårsvurdering.Vurdering

sealed interface BehandlingIverksatt : Søknadsbehandling {
    val vilkårsvurderinger: List<Vurdering>
    val saksbehandler: String
    // TODO Trenger vi flere props/felter?

    companion object {
        fun fromDb(
            id: BehandlingId,
            søknader: List<Søknad>,
            vurderingsperiode: Periode,
            saksopplysninger: List<Saksopplysning>,
            vilkårsvurderinger: List<Vurdering>,
            status: String,
            saksbehandler: String,
        ): BehandlingIverksatt {
            return when (status) {
                "Innvilget" -> Innvilget(
                    id = id,
                    søknader = søknader,
                    vurderingsperiode = vurderingsperiode,
                    saksopplysninger = saksopplysninger,
                    vilkårsvurderinger = vilkårsvurderinger,
                    saksbehandler = saksbehandler,
                )

                "Avslag" -> return Avslag(
                    id = id,
                    søknader = søknader,
                    vurderingsperiode = vurderingsperiode,
                    saksopplysninger = saksopplysninger,
                    vilkårsvurderinger = vilkårsvurderinger,
                    saksbehandler = saksbehandler,
                )

                else -> throw IllegalStateException("Ukjent BehandlingVilkårsvurdert $id med status $status")
            }
        }
    }

    data class Innvilget(
        override val id: BehandlingId,
        override val søknader: List<Søknad>,
        override val vurderingsperiode: Periode,
        override val saksopplysninger: List<Saksopplysning>,
        override val vilkårsvurderinger: List<Vurdering>,
        override val saksbehandler: String,
    ) : BehandlingIverksatt {
        // trenger denne funksjoner?
    }

    data class Avslag(
        override val id: BehandlingId,
        override val søknader: List<Søknad>,
        override val vurderingsperiode: Periode,
        override val saksopplysninger: List<Saksopplysning>,
        override val vilkårsvurderinger: List<Vurdering>,
        override val saksbehandler: String,
    ) : BehandlingIverksatt {
        // trenger denne funksjoner?
    }
}
