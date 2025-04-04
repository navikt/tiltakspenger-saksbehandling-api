package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId

data class MeldeperiodeBeregninger(
    val verdi: List<MeldeperiodeBeregning>,
) : List<MeldeperiodeBeregning> by verdi {

    private val beregningerSortert by lazy { this.sortedBy { it.beregnet } }

    val beregningerPerKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> by lazy {
        beregningerSortert.groupBy { it.kjedeId }
    }

    val beregningerPerMeldekort: Map<MeldekortId, List<MeldeperiodeBeregning>> by lazy {
        beregningerSortert.groupBy { it.meldekortId }
    }

    init {
        verdi.zipWithNext { a, b ->
            require(a.kjedeId == b.kjedeId || a.tilOgMed < b.fraOgMed) {
                "Meldekortperiodene må være sammenhengende og sortert, men var ${verdi.map { it.periode }}"
            }
        }
    }

    companion object {
        fun fraMeldekortBehandlinger(meldekortBehandlinger: MeldekortBehandlinger): MeldeperiodeBeregninger {
            return MeldeperiodeBeregninger(
                meldekortBehandlinger.godkjenteMeldekort.flatMap { it.beregning.beregninger },
            )
        }
    }
}
