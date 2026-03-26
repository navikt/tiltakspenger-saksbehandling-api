package no.nav.tiltakspenger.saksbehandling.beregning.infra.repo

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.SimuleringDbJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toSimuleringDbJson

private data class UtbetalingskontrollDbJson(
    val beregning: BeregningDbJson,
    val simulering: SimuleringDbJson,
)

fun Utbetalingskontroll.tilDbJson(): String {
    return UtbetalingskontrollDbJson(
        beregning = this.beregning.tilBeregningDbJson(),
        simulering = this.simulering.toSimuleringDbJson(),
    ).let { serialize(it) }
}

fun String.tilRammebehandlingUtbetalingskontroll(id: BehandlingId, meldeperiodekjeder: MeldeperiodeKjeder): Utbetalingskontroll {
    val dbJson = deserialize<UtbetalingskontrollDbJson>(this)

    return Utbetalingskontroll(
        beregning = dbJson.beregning.tilBeregningFraRammebehandling(id),
        simulering = dbJson.simulering.toDomain(meldeperiodekjeder),
    )
}
