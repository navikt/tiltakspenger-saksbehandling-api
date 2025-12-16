package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.erFastHelligdag
import no.nav.tiltakspenger.saksbehandling.felles.erHverdag
import java.time.Clock
import java.time.LocalTime

object Åpningstider {
    /**
     * For å kunne si ca. når øknomisystemet er åpent. Tar ikke høyde for bevegelige helligdager og andre edge caser
     * (som at det patches i helger, eller øknomisystemet er åpent utenom ordinær åpningstider)
     * https://helved-docs.intern.dev.nav.no/v3/doc/faq
     */
    fun erInnenforØkonomisystemetsÅpningstider(clock: Clock): Boolean {
        val nå = nå(clock)
        val dato = nå.toLocalDate()
        val klokkeslett = nå.toLocalTime()
        return dato.erHverdag() &&
            !dato.erFastHelligdag() &&
            !klokkeslett.isBefore(LocalTime.of(6, 10)) &&
            klokkeslett.isBefore(LocalTime.of(20, 50))
    }
}
