package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo

import com.fasterxml.jackson.databind.node.TextNode
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.utsjekk.kontrakter.felles.Satstype
import no.nav.utsjekk.kontrakter.iverksett.StønadsdataTiltakspengerV2Dto
import no.nav.utsjekk.kontrakter.iverksett.UtbetalingV2Dto

fun SendtUtbetaling.toJson(): String {
    return serialiserRequestResponse(
        request = this.request,
        response = this.response,
        responseStatus = this.responseStatus,
    )
}

fun KunneIkkeUtbetale.toJson(): String {
    return serialiserRequestResponse(
        request = this.request,
        response = this.response,
        responseStatus = this.responseStatus,
    )
}

private fun serialiserRequestResponse(
    request: String?,
    response: String?,
    responseStatus: Int?,
): String {
    return """
        {
        "request": ${request?.toValidJson()},
        "response": ${response?.toValidJson()},
        "responseStatus": $responseStatus
        }
    """.trimIndent()
}

private fun String.toValidJson(): String {
    if (this.isBlank()) return "\"\""
    val isValidJson = try {
        objectMapper.readTree(this)
        true
    } catch (e: Exception) {
        false
    }
    return if (isValidJson) this else TextNode(this).toString()
}

fun String.tilSatstypePeriodisering(periode: Periode): Periodisering<Satstype> {
    return deserialize<List<UtbetalingV2Dto>>(this).tilSatstypePeriodisering(periode)
}

fun List<UtbetalingV2Dto>.tilSatstypePeriodisering(periode: Periode): Periodisering<Satstype> {
    return this.mapNotNull {
        // Filtrerer vekk utbetaling av barnetillegg for å hindre duplikate perioder
        // Utbetaling av barnetillegg har alltid en tilsvarende utbetaling for ordinært beløp
        if ((it.stønadsdata as StønadsdataTiltakspengerV2Dto).barnetillegg) {
            return@mapNotNull null
        }

        val utbetalingPeriode = Periode(it.fraOgMedDato, it.tilOgMedDato)

        if (!utbetalingPeriode.overlapperMed(periode)) {
            return@mapNotNull null
        }

        PeriodeMedVerdi(
            it.satstype,
            utbetalingPeriode,
        )
    }.let { Periodisering(it) }
}
