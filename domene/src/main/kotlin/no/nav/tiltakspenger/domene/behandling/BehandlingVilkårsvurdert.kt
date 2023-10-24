package no.nav.tiltakspenger.domene.behandling

import mu.KotlinLogging
import no.nav.tiltakspenger.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.felles.SakId
import no.nav.tiltakspenger.vedtak.Søknad
import no.nav.tiltakspenger.vilkårsvurdering.Utfall
import no.nav.tiltakspenger.vilkårsvurdering.Vilkår
import no.nav.tiltakspenger.vilkårsvurdering.Vurdering

val log = KotlinLogging.logger {}

sealed interface BehandlingVilkårsvurdert : Søknadsbehandling {
    val vilkårsvurderinger: List<Vurdering>

    override fun søknad(): Søknad {
        return søknader.maxBy { it.opprettet }
    }

    fun hentUtfallForVilkår(vilkår: Vilkår): Utfall {
        if (vilkårsvurderinger.any { it.vilkår == vilkår && it.utfall == Utfall.KREVER_MANUELL_VURDERING }) return Utfall.KREVER_MANUELL_VURDERING
        if (vilkårsvurderinger.any { it.vilkår == vilkår && it.utfall == Utfall.IKKE_OPPFYLT }) return Utfall.IKKE_OPPFYLT
        if (vilkårsvurderinger.filter { it.vilkår == vilkår }.all { it.utfall == Utfall.OPPFYLT }) return Utfall.OPPFYLT
        throw IllegalStateException("Kunne ikke finne utfall for vilkår $vilkår")
    }

    fun vurderPåNytt(): BehandlingVilkårsvurdert {
        return Søknadsbehandling.Opprettet(
            id = id,
            sakId = sakId,
            søknader = søknader,
            vurderingsperiode = vurderingsperiode,
            saksopplysninger = saksopplysninger,
        ).vilkårsvurder()
    }

    companion object {
        fun fromDb(
            id: BehandlingId,
            sakId: SakId,
            søknader: List<Søknad>,
            vurderingsperiode: Periode,
            saksopplysninger: List<Saksopplysning>,
            vilkårsvurderinger: List<Vurdering>,
            status: String,
        ): BehandlingVilkårsvurdert {
            when (status) {
                "Innvilget" -> return Innvilget(
                    id = id,
                    sakId = sakId,
                    søknader = søknader,
                    vurderingsperiode = vurderingsperiode,
                    saksopplysninger = saksopplysninger,
                    vilkårsvurderinger = vilkårsvurderinger,
                )

                "Avslag" -> return Avslag(
                    id = id,
                    sakId = sakId,
                    søknader = søknader,
                    vurderingsperiode = vurderingsperiode,
                    saksopplysninger = saksopplysninger,
                    vilkårsvurderinger = vilkårsvurderinger,
                )

                "Manuell" -> return Manuell(
                    id = id,
                    sakId = sakId,
                    søknader = søknader,
                    vurderingsperiode = vurderingsperiode,
                    saksopplysninger = saksopplysninger,
                    vilkårsvurderinger = vilkårsvurderinger,
                )

                else -> throw IllegalStateException("Ukjent BehandlingVilkårsvurdert $id med status $status")
            }
        }
    }

    data class Innvilget(
        override val id: BehandlingId,
        override val sakId: SakId,
        override val søknader: List<Søknad>,
        override val vurderingsperiode: Periode,
        override val saksopplysninger: List<Saksopplysning>,
        override val vilkårsvurderinger: List<Vurdering>,
    ) : BehandlingVilkårsvurdert {
        fun iverksett(): BehandlingIverksatt.Innvilget {
            return BehandlingIverksatt.Innvilget(
                id = id,
                sakId = sakId,
                søknader = søknader,
                vurderingsperiode = vurderingsperiode,
                saksopplysninger = saksopplysninger,
                vilkårsvurderinger = vilkårsvurderinger,
                saksbehandler = "Automatisk",
                beslutter = "Automatisk",
            )
        }

        fun tilBeslutting(saksbehandler: String): BehandlingTilBeslutter.Innvilget {
            return BehandlingTilBeslutter.Innvilget(
                id = id,
                sakId = sakId,
                søknader = søknader,
                vurderingsperiode = vurderingsperiode,
                saksopplysninger = saksopplysninger,
                vilkårsvurderinger = vilkårsvurderinger,
                saksbehandler = saksbehandler,
            )
        }

        override fun leggTilSaksopplysning(saksopplysning: Saksopplysning): Søknadsbehandling =
            this.copy(
                saksopplysninger = saksopplysninger
                    .filterNot { it.vilkår == saksopplysning.vilkår && it.kilde == Kilde.SAKSB }
                    .filterNot { it.vilkår == saksopplysning.vilkår && it.kilde == saksopplysning.kilde } +
                    saksopplysning,
            ).vurderPåNytt()
    }

    data class Avslag(
        override val id: BehandlingId,
        override val sakId: SakId,
        override val søknader: List<Søknad>,
        override val vurderingsperiode: Periode,
        override val saksopplysninger: List<Saksopplysning>,
        override val vilkårsvurderinger: List<Vurdering>,
    ) : BehandlingVilkårsvurdert {
        fun iverksett(): BehandlingIverksatt.Avslag {
            return BehandlingIverksatt.Avslag(
                id = id,
                sakId = sakId,
                søknader = søknader,
                vurderingsperiode = vurderingsperiode,
                saksopplysninger = saksopplysninger,
                vilkårsvurderinger = vilkårsvurderinger,
                saksbehandler = "Automatisk",
                beslutter = "Automatisk",
            )
        }

        fun tilBeslutting(saksbehandler: String): BehandlingTilBeslutter.Avslag {
            return BehandlingTilBeslutter.Avslag(
                id = id,
                sakId = sakId,
                søknader = søknader,
                vurderingsperiode = vurderingsperiode,
                saksopplysninger = saksopplysninger,
                vilkårsvurderinger = vilkårsvurderinger,
                saksbehandler = saksbehandler,
            )
        }

        override fun leggTilSaksopplysning(saksopplysning: Saksopplysning): Søknadsbehandling =
            this.copy(
                saksopplysninger = saksopplysninger
                    .filterNot { it.vilkår == saksopplysning.vilkår && it.kilde == Kilde.SAKSB }
                    .filterNot { it.vilkår == saksopplysning.vilkår && it.kilde == saksopplysning.kilde } +
                    saksopplysning,
            ).vurderPåNytt()
    }

    data class Manuell(
        override val id: BehandlingId,
        override val sakId: SakId,
        override val søknader: List<Søknad>,
        override val vurderingsperiode: Periode,
        override val saksopplysninger: List<Saksopplysning>,
        override val vilkårsvurderinger: List<Vurdering>,
    ) : BehandlingVilkårsvurdert {

        override fun leggTilSaksopplysning(saksopplysning: Saksopplysning): Søknadsbehandling =
            this.copy(
                saksopplysninger = saksopplysninger
                    .filterNot { it.vilkår == saksopplysning.vilkår && it.kilde == Kilde.SAKSB }
                    .filterNot { it.vilkår == saksopplysning.vilkår && it.kilde == saksopplysning.kilde } +
                    saksopplysning,
            ).vurderPåNytt()
    }
}
