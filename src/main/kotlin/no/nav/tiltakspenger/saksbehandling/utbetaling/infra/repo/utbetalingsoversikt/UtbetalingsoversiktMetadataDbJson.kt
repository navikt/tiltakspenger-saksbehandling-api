package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
import java.time.LocalDateTime

private data class UtbetalingsoversiktMetadataDbJson(
    val request: String,
    val response: String?,
    val statusKode: Int?,
    val correlationId: String,
    val requestSendt: LocalDateTime?,
    val responsMottatt: LocalDateTime?,
    val varighetMs: Long,
    val antallForsøk: Int,
)

fun UtbetalingsoversiktMetadata.toDbJson(): String = serialize(
    UtbetalingsoversiktMetadataDbJson(
        request = request,
        response = response,
        statusKode = statusKode,
        correlationId = correlationId.value,
        requestSendt = requestSendt,
        responsMottatt = responsMottatt,
        varighetMs = varighet.inWholeMilliseconds,
        antallForsøk = antallForsøk,
    ),
)
