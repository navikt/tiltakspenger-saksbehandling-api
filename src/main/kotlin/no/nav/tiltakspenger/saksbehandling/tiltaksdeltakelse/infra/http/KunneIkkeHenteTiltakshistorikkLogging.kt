package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk

/**
 * Logger hentefeilen én gang, med samme deling mellom vanlig logg og sikkerlogg som `httpklient` sin egen [loggFeil].
 * Kall-feilene bærer hver sin [no.nav.tiltakspenger.libs.httpklient.HttpKlientError] og delegerer dit.
 * [KunneIkkeHenteTiltakshistorikk.UgyldigRespons] har ingen HTTP-feil — beskrivelsen er vår egen og trygg i vanlig logg, mens rå request/respons kun hører hjemme i sikkerlogg.
 */
fun KunneIkkeHenteTiltakshistorikk.loggFeil(
    logger: KLogger,
    operasjon: String,
    kontekst: String,
) {
    when (this) {
        is KunneIkkeHenteTiltakshistorikk.IdentoppslagFeilet ->
            httpKlientError.loggFeil(logger, "$operasjon (identoppslag mot PDL)", kontekst)

        is KunneIkkeHenteTiltakshistorikk.KallFeilet ->
            httpKlientError.loggFeil(logger, operasjon, kontekst)

        is KunneIkkeHenteTiltakshistorikk.UgyldigRespons -> {
            logger.error { "Feil ved $operasjon. $kontekst. $beskrivelse. Se sikkerlogg for detaljer." }
            Sikkerlogg.error { "Feil ved $operasjon. $kontekst. $beskrivelse. Request: ${metadata.rawRequestString}. Response: ${metadata.rawResponseString}." }
        }
    }
}
