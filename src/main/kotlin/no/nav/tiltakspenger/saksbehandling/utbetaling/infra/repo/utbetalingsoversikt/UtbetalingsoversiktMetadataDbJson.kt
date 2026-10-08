package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Avgrensningsårsak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktavgrensning
import java.time.LocalDateTime

private data class UtbetalingsoversiktMetadataDbJson(
    val request: String,
    val svar: String?,
    val mottattSvarSha256: String?,
    val mottattSvarLengde: Int?,
    val statuskode: Int?,
    val correlationId: String,
    val requestSendt: LocalDateTime?,
    val responsMottatt: LocalDateTime?,
    val varighetMs: Long,
    val antallForsøk: Int,
    val avgrensning: AvgrensningDbJson?,
)

private data class AvgrensningDbJson(
    val regelversjon: Int,
    val perioder: List<PeriodeDTO>,
    val antallUtbetalingerMottatt: Int,
    val antallYtelserMottatt: Int,
    val fjernedeYtelserPerÅrsak: Map<String, Int>,
    val antallFjernedeUtbetalinger: Int,
)

fun UtbetalingsoversiktMetadata.toDbJson(): String = serialize(
    UtbetalingsoversiktMetadataDbJson(
        request = request,
        svar = svar,
        mottattSvarSha256 = mottattSvarSha256,
        mottattSvarLengde = mottattSvarLengde,
        statuskode = statuskode,
        correlationId = correlationId.value,
        requestSendt = requestSendt,
        responsMottatt = responsMottatt,
        varighetMs = varighet.inWholeMilliseconds,
        antallForsøk = antallForsøk,
        avgrensning = avgrensning?.toDbJson(),
    ),
)

private fun Utbetalingsoversiktavgrensning.toDbJson() = AvgrensningDbJson(
    regelversjon = regelversjon,
    perioder = perioder.map(Periode::toDTO),
    antallUtbetalingerMottatt = antallUtbetalingerMottatt,
    antallYtelserMottatt = antallYtelserMottatt,
    fjernedeYtelserPerÅrsak = fjernedeYtelserPerÅrsak.mapKeys { (årsak, _) -> årsak.toDb() },
    antallFjernedeUtbetalinger = antallFjernedeUtbetalinger,
)

private fun Avgrensningsårsak.toDb(): String = when (this) {
    Avgrensningsårsak.UTEN_YTELSESTYPE -> "UTEN_YTELSESTYPE"
    Avgrensningsårsak.ANNEN_YTELSESTYPE -> "ANNEN_YTELSESTYPE"
    Avgrensningsårsak.UTENFOR_PERIODENE -> "UTENFOR_PERIODENE"
    Avgrensningsårsak.ANNEN_RETTIGHETSHAVER -> "ANNEN_RETTIGHETSHAVER"
}
