package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface RevurderingResultat : BehandlingResultat {
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
    ) : RevurderingResultat {
        fun valider(status: Behandlingsstatus, virkningsperiode: Periode?) {
            when (status) {
                Behandlingsstatus.KLAR_TIL_BESLUTNING,
                Behandlingsstatus.UNDER_BESLUTNING,
                Behandlingsstatus.VEDTATT,
                -> {
                    require(valgtHjemmel.isNotEmpty()) {
                        "Valgt hjemmel må ha minst ett element for status $status."
                    }
                }
                Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                Behandlingsstatus.KLAR_TIL_BEHANDLING,
                Behandlingsstatus.UNDER_BEHANDLING,
                Behandlingsstatus.AVBRUTT,
                -> Unit
            }
        }
    }

    data class Innvilgelse(
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode>?,
    ) : BehandlingResultat.Innvilgelse,
        RevurderingResultat
}
