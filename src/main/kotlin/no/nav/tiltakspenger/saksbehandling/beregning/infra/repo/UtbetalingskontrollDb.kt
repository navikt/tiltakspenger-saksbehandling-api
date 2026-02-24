package no.nav.tiltakspenger.saksbehandling.beregning.infra.repo

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toDbJson
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.toSimuleringFraDbJson

private data class UtbetalingskontrollDbJson(
    val beregning: String,
    val simulering: String,
)

fun Utbetalingskontroll.tilDbJson(): String {
    return UtbetalingskontrollDbJson(
        beregning = this.beregning.tilBeregningerDbJson(),
        simulering = this.simulering.toDbJson(),
    ).let { serialize(it) }
}

fun String.tilRammebehandlingUtbetalingskontroll(id: BehandlingId, meldeperiodekjeder: MeldeperiodeKjeder): Utbetalingskontroll {
    val dbJson = deserialize<UtbetalingskontrollDbJson>(this)

    return Utbetalingskontroll(
        beregning = Beregning(
            dbJson.beregning.tilMeldeperiodeBeregningerFraBehandling(id),
        ),
        simulering = dbJson.simulering.toSimuleringFraDbJson(meldeperiodekjeder),
    )
}
