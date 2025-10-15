package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser

sealed interface RevurderingResultat : BehandlingResultat {

    /**
     * Når man oppretter en revurdering til stans, lagres det før saksbehandler tar stilling til disse feltene.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * Virkningsperioden/vedtaksperioden og innvilgelsesperioden vil være 1-1 ved denne revurderingstypen.
     *
     * @param harValgtStansFraFørsteDagSomGirRett Dersom saksbehandler har valgt at det skal stanses fra første dag som gir rett. Vil være null når man oppretter stansen.
     * @param harValgtStansTilSisteDagSomGirRett Dersom saksbehandler har valgt at det skal stanses til siste dag som gir rett. Vil være null når man oppretter stansen.
     */
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
        val harValgtStansTilSisteDagSomGirRett: Boolean?,
        val stansperiode: Periode?,
    ) : RevurderingResultat {

        override val virkningsperiode = stansperiode
        override val barnetillegg = null
        override val valgteTiltaksdeltakelser = null
        override val antallDagerPerMeldeperiode = null

        fun valider() {
            require(valgtHjemmel.isNotEmpty()) {
                "Valgt hjemmel må ha minst ett element"
            }
        }

        companion object {
            val empty: Stans = Stans(
                valgtHjemmel = emptyList(),
                harValgtStansFraFørsteDagSomGirRett = null,
                harValgtStansTilSisteDagSomGirRett = null,
                stansperiode = null,
            )
        }
    }

    /**
     * Når man oppretter en revurdering og velger innvilgelse, har man ikke tatt stilling til disse feltene ennå.
     * Alle bør være satt når behandlingen er til beslutning.
     *
     * Virkningsperioden/vedtaksperioden og innvilgelsesperioden vil være 1-1 ved denne revurderingstypen.
     */
    data class Innvilgelse(
        override val valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser?,
        override val barnetillegg: Barnetillegg?,
        override val antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode>?,
        val innvilgelsesperiode: Periode?,
    ) : BehandlingResultat.Innvilgelse,
        RevurderingResultat {
        override val virkningsperiode = innvilgelsesperiode

        fun nullstill() = empty

        companion object {
            val empty = Innvilgelse(
                valgteTiltaksdeltakelser = null,
                barnetillegg = null,
                antallDagerPerMeldeperiode = null,
                innvilgelsesperiode = null,
            )
        }
    }
}
