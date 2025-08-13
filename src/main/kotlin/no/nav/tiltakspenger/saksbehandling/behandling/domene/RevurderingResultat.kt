package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface RevurderingResultat : BehandlingResultat {
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
    ) : RevurderingResultat {

        fun valider() {
            require(valgtHjemmel.isNotEmpty()) {
                "Valgt hjemmel må ha minst ett element"
            }
        }
    }

    data class Innvilgelse(
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
        val utbetaling: Utbetaling?,
    ) : BehandlingResultat.Innvilgelse,
        RevurderingResultat {

        data class Utbetaling(
            val beregning: BehandlingBeregning,
            val navkontor: Navkontor,
        )
    }
}
