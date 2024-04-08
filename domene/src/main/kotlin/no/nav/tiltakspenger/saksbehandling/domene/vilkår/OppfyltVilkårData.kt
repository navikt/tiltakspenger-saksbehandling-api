package no.nav.tiltakspenger.saksbehandling.domene.vilkår

import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.YtelseSaksopplysning
import java.time.LocalDateTime

data class OppfyltVilkårData(
    val vilkår: Vilkår,
    val vurderingsperiode: Periode,
    val saksopplysningerSaksbehandler: YtelseSaksopplysningerForEnKilde,
    val saksopplysningerAnnet: YtelseSaksopplysningerForEnKilde,
    val avklarteFakta: YtelseSaksopplysningerForEnKilde,
    val vurderinger: List<Vurdering>,
) {
    fun leggTilSaksopplysning(saksopplysning: Saksopplysning) {
        // TODO Her må vi enten få inn hele sannheten eller legge denne ene til i listen og lage de implisitte
    }

    fun avklarFakta(): OppfyltVilkårData {
        return this.copy(
            avklarteFakta = if (saksopplysningerSaksbehandler.erSatt()) saksopplysningerSaksbehandler else saksopplysningerAnnet,
        )
    }

    fun vilkårsvurder(): OppfyltVilkårData {
        val vurderinger = avklarteFakta.saksopplysninger.map { it.lagVurdering() } +
            vurderingsperiode.ikkeOverlappendePerioder(vurderinger.map { it.periode() }).map {
                // TODO lag enten en saksopplysning.lagVurder eller lag Vurdering her
                // TODO avklar foretningsreglene her
                //      hvis utvidet i slutten av perioden og er oppfylt kan man sette oppfylt for perioden?
                //      hvis utvidet i slutten av perioden og er ikke oppfylt setter man til manuell?
                //      er det mulig å endre i starten?
            }

        return this
        // TODO()
//        return this.copy(
//            vurderinger = vurderinger
//        )
    }
}

data class YtelseSaksopplysningerForEnKilde(
    val kilde: Kilde,
    val periode: Periode,
    val saksopplysninger: List<YtelseSaksopplysning>,
    val tidspunkt: LocalDateTime,
) {
    init {
        if (kilde == Kilde.SAKSB) {
            saksopplysninger.forEach { saksopplysning ->
                require(periode.inneholderHele(saksopplysning.periode()))
                { "Saksopplysning periode ${saksopplysning.periode()} har ikke lov å være utenfor perioden $periode" }
            }

        }
    }
    init {
        // check(periode) sjekk at saksopplysninger dekker hele perioden når man oppretter den hvis det er saksbehandler
        // check at saksopplysninger ikke overlapper
        // check at saksopplysninger ikke går ut over perioden
        // check at det ikke er huller i listen
    }

    fun erSatt() = saksopplysninger.isNotEmpty()
}
