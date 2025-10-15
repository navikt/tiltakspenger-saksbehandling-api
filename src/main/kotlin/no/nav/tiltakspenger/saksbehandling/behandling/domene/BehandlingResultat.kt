package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface BehandlingResultat {

    /** Kan være null ved sære tilfeller av avslag, og når behandlingen er uferdig */
    val virkningsperiode: Periode?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val barnetillegg: Barnetillegg?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?

    /** Vil være null ved stans og når behandlingen er uferdig */
    val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?

    /** Denne benyttes både i søknadsbehandlinger og revurderinger */
    sealed interface Innvilgelse : BehandlingResultat {

        fun valider(virkningsperiode: Periode?) {
            requireNotNull(virkningsperiode) {
                "Virkningsperiode må være satt for innvilget behandling"
            }

            valgteTiltaksdeltakelser.also {
                requireNotNull(it) {
                    "Valgte tiltaksdeltakelser må være satt for innvilget behandling"
                }

                require(it.periodisering.totalPeriode == virkningsperiode) {
                    "Total periode for valgte tiltaksdeltakelser (${it.periodisering.totalPeriode}) må stemme overens med virkningsperioden ($virkningsperiode)"
                }
            }

            barnetillegg.also {
                requireNotNull(it) {
                    "Barnetillegg må være satt for innvilget behandling"
                }

                val barnetilleggsperiode = it.periodisering.totalPeriode
                require(barnetilleggsperiode == virkningsperiode) {
                    "Barnetilleggsperioden ($barnetilleggsperiode) må ha samme periode som virkningsperioden($virkningsperiode)"
                }
            }

            antallDagerPerMeldeperiode.also {
                requireNotNull(it) {
                    "antallDagerPerMeldeperiode må være satt for innvilget behandling"
                }

                require(it.totalPeriode == virkningsperiode) {
                    "Innvilgelsesperioden ($virkningsperiode) må være lik som antallDagerPerMeldeperiode sin totale periode (${it.totalPeriode})"
                }
            }
        }
    }
}

sealed interface BehandlingResultatType

enum class SøknadsbehandlingType : BehandlingResultatType {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingType : BehandlingResultatType {
    STANS,
    INNVILGELSE,
}
