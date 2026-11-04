package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response

import io.micrometer.core.instrument.MeterRegistry

/**
 * Teller ytelser fra sokos-utbetaldata med en type som verken står i [henteteYtelsestyper] eller i [utelatteYtelsestyper], per type og uten sak.
 * Slike ytelser blir [no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse.Ukjent] på behandlingen; varselet «Ukjent ytelsestype fra utbetaldata» i tiltakspenger-iac fyrer på telleren.
 * Tallet sier bare om typen er sett, ikke hvor mange personer som har den, fordi samme person slås opp flere ganger.
 */
fun MeterRegistry.tellUkjenteYtelsestyper(ytelsestyper: List<String?>) {
    ytelsestyper.filterNotNull().filter { it.erUkjentYtelsestype() }.groupingBy { it }.eachCount().forEach { (ytelsestype, antall) ->
        counter("tpts_saksbehandlingapi_utbetaldata_ukjente_ytelser", "ytelsestype", ytelsestype).increment(antall.toDouble())
    }
}

/** Sann når teksten verken er en type vi henter eller en vi har valgt å utelate. */
fun String.erUkjentYtelsestype(): Boolean = tilYtelsetype() == null && this !in utelatteYtelsestyper
