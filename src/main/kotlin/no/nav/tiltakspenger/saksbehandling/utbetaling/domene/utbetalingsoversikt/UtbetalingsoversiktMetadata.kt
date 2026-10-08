package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.time.Duration

/**
 * Request, svar og tider fra ett kall, lagret for notoritet og feilsøking.
 * Systemet leser dem ikke, og databasen får aldri et svar med innhold vi ikke har grunnlag for.
 * Ved vellykket kall lagres [Utbetalingsoversiktsvar.avgrensetSvar], og ved svar med annen statuskode enn 200 lagres feilteksten.
 * Et 200-svar vi ikke kunne tolke lagres bare som [mottattSvarSha256] og [mottattSvarLengde], mens innholdet står i sikkerlogg.
 *
 * @param antallForsøk Blir 2 når httpklient har hentet ferskt token etter en 401.
 */
data class UtbetalingsoversiktMetadata(
    val request: String,
    val svar: String?,
    val mottattSvarSha256: String?,
    val mottattSvarLengde: Int?,
    val statuskode: Int?,
    val correlationId: CorrelationId,
    val requestSendt: LocalDateTime?,
    val responsMottatt: LocalDateTime?,
    val varighet: Duration,
    val antallForsøk: Int,
    val avgrensning: Utbetalingsoversiktavgrensning?,
)

fun Utbetalingsoversiktsvar.tilUtbetalingsoversiktMetadata(correlationId: CorrelationId): UtbetalingsoversiktMetadata =
    metadata.tilUtbetalingsoversiktMetadata(correlationId).copy(svar = avgrensetSvar, avgrensning = avgrensning)

/** For feilede kall; se [UtbetalingsoversiktMetadata]. */
fun HttpKlientMetadata.tilUtbetalingsoversiktMetadata(correlationId: CorrelationId): UtbetalingsoversiktMetadata =
    UtbetalingsoversiktMetadata(
        request = rawRequestString,
        svar = rawResponseString?.takeIf { statusCode != 200 },
        mottattSvarSha256 = rawResponseString?.sha256(),
        mottattSvarLengde = rawResponseString?.length,
        statuskode = statusCode,
        correlationId = correlationId,
        requestSendt = tidsstempler.requestSendt,
        responsMottatt = tidsstempler.responsMottatt,
        varighet = totalDuration,
        antallForsøk = attempts,
        avgrensning = null,
    )

fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray()).joinToString("") { "%02x".format(it) }
