package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Avgrensningsårsak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.MottattYtelsestype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktavgrensning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktgrunnlag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.sha256
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.erUkjentYtelsestype

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
    val fjernede = vurderte.flatMap { (_, ytelser) -> ytelser.mapNotNull { (ytelse, årsak) -> årsak?.let { ytelse to it } } }
    return AvgrensetSvar(
        utbetalinger = utbetalinger,
        avgrensning = Utbetalingsoversiktavgrensning(
            regelversjon = Utbetalingsoversiktgrunnlag.REGELVERSJON,
            perioder = grunnlag.perioder,
            antallUtbetalingerMottatt = size,
            antallYtelserMottatt = sumOf { it.ytelseListe.size },
            fjernedeYtelserPerÅrsak = fjernede.groupingBy { (_, årsak) -> årsak }.eachCount(),
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
        grunnlag.avgrensningsårsak(
            ytelsestype = ytelsestype?.tilMottattYtelsestype(),
            periode = Periode(ytelsesperiode.fom, ytelsesperiode.tom),
            rettighetshaver = rettighetshaver?.ident?.let { Fnr.tryFromString(it) },
        )
    }

/** Tjenesten sender beskrivelsen av ytelsen, ikke en kode. */
private fun String.tilMottattYtelsestype(): MottattYtelsestype {
    val ytelsetype = Ytelsetype.entries.find { it != Ytelsetype.UKJENT && it.tekstverdi == this }
    return when {
        ytelsetype != null -> MottattYtelsestype.Relevant(ytelsetype)
        erUkjentYtelsestype() -> MottattYtelsestype.Ukjent(this)
        else -> MottattYtelsestype.Utelatt(this)
    }
}

private fun UtbetalingsoversiktDto.AktoerDto.utenNavn() = copy(navn = null)
