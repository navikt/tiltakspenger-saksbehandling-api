package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface SøknadsbehandlingUtfall {
    data class Innvilgelse(
        val status: Behandlingsstatus,
        val virkningsperiode: Periode,
        val antallDagerPerMeldeperiode: Int,
        val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        val barnetillegg: Barnetillegg?,
    ) : SøknadsbehandlingUtfall {

        val utfallsperioder: Periodisering<Utfallsperiode> =
            Periodisering(Utfallsperiode.RETT_TIL_TILTAKSPENGER, virkningsperiode)

        init {
            require(antallDagerPerMeldeperiode in 1..14) {
                "Antall dager per meldeperiode må være mellom 1 og 14"
            }

            when (status) {
                Behandlingsstatus.KLAR_TIL_BESLUTNING,
                Behandlingsstatus.UNDER_BESLUTNING,
                Behandlingsstatus.VEDTATT,
                -> {
                    require(valgteTiltaksdeltakelser != null) { "Valgte tiltaksdeltakelser må være satt for førstegangsbehandling" }
                    require(valgteTiltaksdeltakelser.periodisering.totalePeriode == virkningsperiode) {
                        "Total periode for valgte tiltaksdeltakelser (${valgteTiltaksdeltakelser.periodisering.totalePeriode}) må stemme overens med virkningsperioden ($virkningsperiode)"
                    }

                    if (barnetillegg != null) {
                        val barnetilleggsperiode = barnetillegg.periodisering.totalePeriode
                        require(barnetilleggsperiode == virkningsperiode) {
                            "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)"
                        }
                    }
                }

                Behandlingsstatus.KLAR_TIL_BEHANDLING,
                Behandlingsstatus.UNDER_BEHANDLING,
                Behandlingsstatus.AVBRUTT,
                -> Unit
            }
        }
    }

    data class Avslag(
        val avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    ) : SøknadsbehandlingUtfall
}
