package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import ulid.ULID
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BeregningId private constructor(private val ulid: UlidBase) : Ulid by ulid {
    companion object {
        private const val PREFIX = "beregning"
        fun random() = BeregningId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): BeregningId {
            require(stringValue.startsWith(PREFIX)) { "Prefix må starte med $PREFIX. Dette er nok ikke en BeregningId ($stringValue)" }
            return BeregningId(ulid = UlidBase(stringValue))
        }

        fun fromString(uuid: UUID) = BeregningId(ulid = UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}

/** @param meldekortId Id for meldekort-behandlingen med utfylte dager for beregningen av denne perioden
 *  */
sealed interface MeldeperiodeBeregning {
    val id: BeregningId

    val meldekortId: MeldekortId
    val kjedeId: MeldeperiodeKjedeId
    val dager: NonEmptyList<MeldeperiodeBeregningDag>
    val iverksattTidspunkt: LocalDateTime?

    val fraOgMed: LocalDate get() = dager.first().dato
    val tilOgMed: LocalDate get() = dager.last().dato
    val periode: Periode get() = Periode(fraOgMed, tilOgMed)

    fun init() {
        require(dager.size == 14) { "En meldeperiode må være 14 dager, men var ${dager.size}" }
        require(dager.first().dato.dayOfWeek == DayOfWeek.MONDAY) { "Meldeperioden må starte på en mandag" }
        require(dager.last().dato.dayOfWeek == DayOfWeek.SUNDAY) { "Meldeperioden må slutte på en søndag" }
        dager.forEachIndexed { index, dag ->
            require(dager.first().dato.plusDays(index.toLong()) == dag.dato) {
                "Datoene må være sammenhengende og sortert, men var ${dager.map { it.dato }}"
            }
        }
    }

    fun beregnTotalOrdinærBeløp(): Int = dager.sumOf { it.beregningsdag?.beløp ?: 0 }

    fun beregnTotalBarnetillegg(): Int = dager.sumOf { it.beregningsdag?.beløpBarnetillegg ?: 0 }

    fun beregnTotaltBeløp(): Int = beregnTotalOrdinærBeløp() + beregnTotalBarnetillegg()
}

/** @param beregnetMeldekortId Id for meldekort-behandlingen som utløste denne beregningen. Denne kan være ulik [meldekortId] for beregninger som er et resultat av en korrigering som påvirket en påfølgende meldeperiode.
 * */
data class MeldeperiodeBeregningFraMeldekort(
    override val id: BeregningId,
    override val kjedeId: MeldeperiodeKjedeId,
    override val meldekortId: MeldekortId,
    override val dager: NonEmptyList<MeldeperiodeBeregningDag>,
    override val iverksattTidspunkt: LocalDateTime?,
    val beregnetMeldekortId: MeldekortId,
) : MeldeperiodeBeregning {

    init {
        super.init()
    }
}

@Suppress("unused")
data class MeldeperiodeBeregningFraBehandling(
    override val id: BeregningId,
    override val kjedeId: MeldeperiodeKjedeId,
    override val meldekortId: MeldekortId,
    override val dager: NonEmptyList<MeldeperiodeBeregningDag>,
    override val iverksattTidspunkt: LocalDateTime?,
    val beregnetBehandlingId: BehandlingId,
) : MeldeperiodeBeregning {

    init {
        super.init()
    }
}
