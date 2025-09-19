package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import java.time.LocalDateTime

private typealias BeregningMedIverksattTidspunkt = Pair<MeldeperiodeBeregning, LocalDateTime>

data class MeldeperiodeBeregninger(
    private val meldekortBehandlinger: MeldekortBehandlinger,
    private val behandlinger: Behandlinger,
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

    val beregningerPerKjede: Map<MeldeperiodeKjedeId, List<MeldeperiodeBeregning>> by lazy {
        meldeperiodeBeregninger.groupBy { it.kjedeId }
    }

    val sisteBeregningPerKjede: Map<MeldeperiodeKjedeId, MeldeperiodeBeregning> by lazy {
        beregningerPerKjede.entries.associate { it.key to it.value.last() }
    }

    val gjeldendeBeregninger: List<MeldeperiodeBeregning> by lazy {
        sisteBeregningPerKjede.values.toList()
    }

    fun sisteBeregningFør(beregningId: BeregningId, kjedeId: MeldeperiodeKjedeId): MeldeperiodeBeregning? {
        return beregningerPerKjede[kjedeId]?.takeWhile { it.id != beregningId }?.lastOrNull()
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
}
