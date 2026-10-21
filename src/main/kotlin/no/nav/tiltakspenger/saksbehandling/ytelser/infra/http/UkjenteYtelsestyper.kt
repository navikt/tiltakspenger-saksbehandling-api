package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Teller ytelser fra sokos-utbetaldata med en type som verken står i [Ytelsetype] eller i [utelatteYtelsestyper], per type og uten sak.
 * Slike ytelser forkastes, så telleren er eneste spor av dem; varselet «Ukjent ytelsestype fra utbetaldata» i tiltakspenger-iac fyrer på den.
 * Tallet sier bare om typen er sett, ikke hvor mange personer som har den, fordi samme person slås opp flere ganger.
 */
fun MeterRegistry.tellUkjenteYtelsestyper(ytelsestyper: List<String?>) {
    ytelsestyper.filterNotNull().filter { it.erUkjentYtelsestype() }.groupingBy { it }.eachCount().forEach { (ytelsestype, antall) ->
        counter("tpts_saksbehandlingapi_utbetaldata_ukjente_ytelser", "ytelsestype", ytelsestype).increment(antall.toDouble())
    }
}

/** Sann når teksten verken er en type vi henter eller en vi har valgt å utelate. */
fun String.erUkjentYtelsestype(): Boolean =
    Ytelsetype.entries.none { it != Ytelsetype.UKJENT && it.tekstverdi == this } && this !in utelatteYtelsestyper
