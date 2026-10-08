package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Avgrensningsårsak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktavgrensning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktgrunnlag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.sha256

/** Svaret slik det lagres og mappes, og tallene på det som ble tatt bort. */
data class AvgrensetSvar(
    val utbetalinger: List<UtbetalingsoversiktDto>,
    val avgrensning: Utbetalingsoversiktavgrensning,
)

/**
 * Tar bort ytelsene utenfor [grunnlag], utbetalingene som da står uten ytelser, og kontonummer og navn.
 * En utbetaling som har mistet ytelser, mister også totalbeløpet sitt, siden det ellers røper summen av det som ble tatt bort.
 * En ytelse med ugyldig periode beholdes, så mappingen får melde kontraktbruddet.
 *
 * @param mottattSvar Svaret slik det kom fra tjenesten, brukt til lengde og sha256.
 */
fun List<UtbetalingsoversiktDto>.avgrensTil(grunnlag: Utbetalingsoversiktgrunnlag, mottattSvar: String): AvgrensetSvar {
    val vurderte = map { utbetaling -> utbetaling to utbetaling.ytelseListe.map { it to it.avgrensningsårsak(grunnlag) } }
    val utbetalinger = vurderte.mapNotNull { (utbetaling, ytelser) ->
        val beholdte = ytelser.filter { (_, årsak) -> årsak == null }.map { (ytelse, _) -> ytelse }
        if (beholdte.isEmpty()) return@mapNotNull null
        utbetaling.copy(
            ytelseListe = beholdte.map { it.copy(rettighetshaver = it.rettighetshaver?.utenNavn(), refundertForOrg = it.refundertForOrg?.utenNavn()) },
            utbetaltTil = utbetaling.utbetaltTil?.utenNavn(),
            utbetaltTilKonto = null,
            utbetalingNettobeloep = utbetaling.utbetalingNettobeloep.takeIf { beholdte.size == ytelser.size },
        )
    }
    return AvgrensetSvar(
        utbetalinger = utbetalinger,
        avgrensning = Utbetalingsoversiktavgrensning(
            regelversjon = Utbetalingsoversiktgrunnlag.REGELVERSJON,
            perioder = grunnlag.perioder,
            antallUtbetalingerMottatt = size,
            antallYtelserMottatt = sumOf { it.ytelseListe.size },
            fjernedeYtelserPerÅrsak = vurderte.flatMap { (_, ytelser) -> ytelser.mapNotNull { (_, årsak) -> årsak } }.groupingBy { it }.eachCount(),
            antallFjernedeUtbetalinger = size - utbetalinger.size,
            mottattSvarSha256 = mottattSvar.sha256(),
            mottattSvarLengde = mottattSvar.length,
        ),
    )
}

private fun UtbetalingsoversiktDto.YtelseDto.avgrensningsårsak(grunnlag: Utbetalingsoversiktgrunnlag): Avgrensningsårsak? =
    if (ytelsesperiode.fom.isAfter(ytelsesperiode.tom)) {
        null
    } else {
        grunnlag.avgrensningsårsak(ytelsestype, Periode(ytelsesperiode.fom, ytelsesperiode.tom), rettighetshaver?.ident)
    }

private fun UtbetalingsoversiktDto.AktoerDto.utenNavn() = copy(navn = null)
