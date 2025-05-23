package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface SøknadsbehandlingUtfall : BehandlingUtfall {
    data class Innvilgelse(
        val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser,
        val barnetillegg: Barnetillegg?,
    ) : SøknadsbehandlingUtfall {

        fun valider(status: Behandlingsstatus, virkningsperiode: Periode?) {
            requireNotNull(virkningsperiode) {
                "Virkningsperiode må være satt for innvilget søknadsbehandling"
            }

            when (status) {
                KLAR_TIL_BESLUTNING,
                UNDER_BESLUTNING,
                VEDTATT,
                -> {
                    require(valgteTiltaksdeltakelser.periodisering.totalPeriode == virkningsperiode) {
                        "Total periode for valgte tiltaksdeltakelser (${valgteTiltaksdeltakelser.periodisering.totalPeriode}) må stemme overens med virkningsperioden ($virkningsperiode)"
                    }

                    if (barnetillegg != null) {
                        val barnetilleggsperiode = barnetillegg.periodisering.totalPeriode
                        require(barnetilleggsperiode == virkningsperiode) {
                            "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)"
                        }
                    }
                }

                KLAR_TIL_BEHANDLING,
                UNDER_BEHANDLING,
                AVBRUTT,
                -> Unit
            }
        }
    }

    data class Avslag(
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : SøknadsbehandlingUtfall
}
