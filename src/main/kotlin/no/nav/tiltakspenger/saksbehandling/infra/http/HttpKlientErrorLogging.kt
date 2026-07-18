package no.nav.tiltakspenger.saksbehandling.infra.http

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.throwableOrNull
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

/**
 * Felles feillogging for konsumenter av `tiltakspenger-libs:httpklient`.
 *
 * Vi lar libs-klienten være stille (ingen `logging { … }` på [no.nav.tiltakspenger.libs.httpklient.HttpKlient]) og logger i stedet én gang her, fra det laget som har domenekonteksten (typisk en service).
 * Slik unngår vi dobbeltlogging (transport-logg fra libs + domenelogg fra oss) og får nøyaktig én logghendelse per feilsituasjon.
 * All HTTP-kontekst hentes fra [HttpKlientError] selv ([HttpKlientError.metadata]), så kalleren trenger bare bidra med det kalleren faktisk vet mer om enn klienten: [operasjon] og [kontekst].
 *
 * En «logghendelse» er her paret [logger].error (uten personopplysninger) + [Sikkerlogg].error (med rå request/respons), i tråd med resten av kodebasen.
 *
 * @param logger Kallerens egen logger, slik at logglinja får kallerens navnrom.
 * @param operasjon Kort beskrivelse av hva som feilet, f.eks. `"sending til datadeling"`.
 * Ulike klient/service-par kan sende sin egen.
 * @param kontekst Domenekontekst som bare kalleren har, f.eks. `"Sak abc, saksnummer 123"`.
 * Bygg strengen slik det gir mening for den konkrete feilsituasjonen.
 */
fun HttpKlientError.loggFeil(
    logger: KLogger,
    operasjon: String,
    kontekst: String,
) {
    val throwable = throwableOrNull()
    val logMelding =
        "Feil ved $operasjon. $kontekst. Status: ${metadata.statusCode}, forsøk: ${metadata.attempts}. Se sikkerlogg for detaljer."
    val sikkerMelding =
        "Feil ved $operasjon. $kontekst. Status: ${metadata.statusCode}, forsøk: ${metadata.attempts}, request: ${metadata.rawRequestString}. response: ${metadata.rawResponseString}. responseHeaders: ${metadata.responseHeaders}."
    if (throwable != null) {
        logger.error(throwable) { logMelding }
        Sikkerlogg.error(throwable) { sikkerMelding }
    } else {
        logger.error { logMelding }
        Sikkerlogg.error { sikkerMelding }
    }
}
