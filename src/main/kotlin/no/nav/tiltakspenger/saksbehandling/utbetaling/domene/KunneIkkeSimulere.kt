package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.rawResponseString
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

/**
 * Feil ved simulering mot helved.
 *
 * [UkjentFeil], [Stengt] og [Timeout] bærer den underliggende [HttpKlientError]-en; den er kun ment for [logg].
 * [UgyldigSimulering] betyr at helved svarte, men at svaret ikke lar seg tolke -- se [Simuleringsfeil] for årsakene.
 */
sealed interface KunneIkkeSimulere {
    data class UkjentFeil(val feil: HttpKlientError) : KunneIkkeSimulere

    /**
     * OS har åpningstider.
     * Typisk mandag til fredag fra 6 til 21.
     * Men det hender den er stengt på helligdager og vedlikeholdsdager også.
     */
    data class Stengt(val feil: HttpKlientError) : KunneIkkeSimulere

    data class Timeout(val feil: HttpKlientError) : KunneIkkeSimulere

    data class UgyldigSimulering(val feil: Simuleringsfeil) : KunneIkkeSimulere
}

/**
 * Logger utfallet på riktig nivå, med én linje per feil.
 *
 * [Stengt] er forventet utenfor åpningstidene til OS og er ikke en feilsituasjon hos oss.
 * [UgyldigSimulering] betyr at svaret bryter kontrakten vi tolker det etter, og logges som error -- det skal en utvikler se på.
 * Linjen stemples med `KunneIkkeSimulere.<utfall>`, slik at man kan søke på `KunneIkkeSimulere` for alle utfall og på f.eks. `UgyldigSimulering` for ett spesifikt.
 */
fun KunneIkkeSimulere.logg(logger: KLogger, kontekst: String) {
    when (this) {
        is KunneIkkeSimulere.Stengt -> {
            logger.debug { "Simulering mot helved: 503. Kontrakten definerer 503 som «OS/UR er midlertidig stengt» -- typisk utenfor åpningstid, på helligdager eller under vedlikehold hos oppdragssystemet. Se sikkerlogg for mer kontekst. $kontekst - KunneIkkeSimulere.Stengt" }
            Sikkerlogg.debug { "Simulering mot helved: 503. OS/UR er midlertidig stengt. $kontekst, body: ${feil.rawResponseString}" }
        }

        is KunneIkkeSimulere.Timeout -> feil.loggFeil(logger, "simulering mot helved (timeout)", kontekst)

        is KunneIkkeSimulere.UkjentFeil -> feil.loggFeil(logger, "ukjent feil ved simulering mot helved", kontekst)

        is KunneIkkeSimulere.UgyldigSimulering -> logger.error(feil.loggkontekst.underliggendeFeil) {
            "Simulering mot helved ga et svar vi ikke kan tolke: ${feil.loggkontekst.melding} $kontekst - KunneIkkeSimulere.UgyldigSimulering(${feil::class.simpleName})"
        }
    }
}
