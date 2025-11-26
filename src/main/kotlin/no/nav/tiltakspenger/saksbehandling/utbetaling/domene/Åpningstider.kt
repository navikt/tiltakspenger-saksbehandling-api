package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.erHverdag
import java.time.Clock
import java.time.LocalTime

object Åpningstider {
    /**
     * For å kunne si ca. når øknomisystemet er åpent. Tar ikke høyde for helligdager og andre edge caser
     * (som at det patches i helger, eller øknomisystemet er åpent utenom ordinær åpningstider)
     * https://helved-docs.intern.dev.nav.no/v3/doc/faq
     */
    fun erInnenforØkonomisystemetsÅpningstider(clock: Clock): Boolean {
        val nå = nå(clock)
        val klokkeslett = nå.toLocalTime()
        return nå.toLocalDate().erHverdag() &&
            !klokkeslett.isBefore(LocalTime.of(6, 0)) &&
            klokkeslett.isBefore(LocalTime.of(21, 0))
    }
}
