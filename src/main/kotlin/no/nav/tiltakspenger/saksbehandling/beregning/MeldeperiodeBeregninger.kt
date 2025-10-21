package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import java.time.LocalDateTime

private typealias BeregningMedIverksattTidspunkt = Pair<MeldeperiodeBeregning, LocalDateTime>

data class MeldeperiodeBeregninger(
    private val meldekortBehandlinger: Meldekortbehandlinger,
    private val behandlinger: Rammebehandlinger,
) {
    private val godkjenteMeldekort: List<MeldekortBehandling.Behandlet> = meldekortBehandlinger.godkjenteMeldekort
        .sortedBy { it.iverksattTidspunkt }

    private val iverksatteBehandlinger: List<Rammebehandling> = behandlinger.filter { it.erVedtatt }

    private val meldeperiodeBeregningerMedTidspunkt: List<BeregningMedIverksattTidspunkt> by lazy {
        val beregningerFraMeldekort = godkjenteMeldekort.flatMap { meldekort ->
            meldekort.beregning.beregninger.map { BeregningMedIverksattTidspunkt(it, meldekort.iverksattTidspunkt!!) }
        }

        val beregningerFraBehandlinger = iverksatteBehandlinger
            .mapNotNull { behandling ->
                behandling.utbetaling?.beregning?.beregninger?.map {
                    BeregningMedIverksattTidspunkt(it, behandling.iverksattTidspunkt!!)
                }?.toList()
            }.flatten()

        beregningerFraMeldekort.plus(beregningerFraBehandlinger).sortedBy { it.second }
    }

    private val meldeperiodeBeregninger: List<MeldeperiodeBeregning> by lazy {
        meldeperiodeBeregningerMedTidspunkt.map { it.first }
    }

    val beregningerPerKjede: Map<MeldeperiodeKjedeId, NonEmptyList<MeldeperiodeBeregning>> by lazy {
        meldeperiodeBeregninger
            .groupBy { it.kjedeId }
            .mapValues { it.value.toNonEmptyListOrThrow() }
    }

    val sisteBeregningPerKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> by lazy {
        beregningerPerKjede.entries.associate { it.key to it.value.last() }
    }

    val gjeldendeBeregninger: List<MeldeperiodeBeregning> by lazy {
        sisteBeregningPerKjede.values.toList()
    }

    fun hentForrigeBeregning(
        beregningId: BeregningId,
        kjedeId: MeldeperiodeKjedeId,
    ): Either<ForrigeBeregningFinnesIkke, MeldeperiodeBeregning> {
        val beregningerForKjede =
            beregningerPerKjede[kjedeId] ?: return ForrigeBeregningFinnesIkke.IngenBeregningerForKjede.left()

        // Finnes ingen forrige beregning hvis dette er den første på kjeden
        if (beregningerForKjede.first().id == beregningId) {
            return ForrigeBeregningFinnesIkke.IngenTidligereBeregninger.left()
        }

        return beregningerForKjede.takeWhile { it.id != beregningId }.let {
            if (it.isEmpty()) {
                ForrigeBeregningFinnesIkke.BeregningFinnesIkke.left()
            } else {
                it.last().right()
            }
        }
    }

    fun sisteBeregningerForPeriode(periode: Periode): List<MeldeperiodeBeregning> {
        return sisteBeregningPerKjede.values.filter { it.periode.overlapperMed(periode) }
    }

    init {
        meldeperiodeBeregningerMedTidspunkt.zipWithNext { a, b ->
            require(a.second <= b.second) {
                "Meldeperiodeberegningene må være sorterte - Fant ${a.first} ${a.second} / ${b.first} ${b.second}"
            }
        }

        val duplikater = meldeperiodeBeregninger.nonDistinctBy { it.id }
        require(duplikater.isEmpty()) {
            "Fant duplikate meldeperiodeberegninger: $duplikater"
        }
    }

    // Tanken var å bruke disse til tester for å skille mellom ulike null-resultater.
    // TODO etter omskrivning av MeldeperiodeBeregninger :D
    enum class ForrigeBeregningFinnesIkke {
        IngenBeregningerForKjede,
        IngenTidligereBeregninger,
        BeregningFinnesIkke,
    }
}
