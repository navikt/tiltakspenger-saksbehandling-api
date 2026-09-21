package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.felles.erFastHelligdag
import no.nav.tiltakspenger.saksbehandling.felles.erHverdag
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Åpningstider {
    /**
     * For å kunne si ca. når øknomisystemet er åpent.
     * Tar ikke høyde for bevegelige helligdager og andre edge caser (som at det patches i helger, eller øknomisystemet er åpent utenom ordinær åpningstider)
     * - https://helved-docs.intern.dev.nav.no/v3/doc/faq
     */
    fun erInnenforØkonomisystemetsÅpningstider(clock: Clock): Boolean = erÅpent(nå(clock))

    /** Finner første tidspunkt fra og med [tidspunkt] der økonomisystemet er åpent. */
    fun førsteÅpneTidspunkt(tidspunkt: LocalDateTime): LocalDateTime {
        val dato = tidspunkt.toLocalDate()
        return when {
            erÅpent(tidspunkt) -> tidspunkt
            dato.erÅpenDag() && tidspunkt.toLocalTime().isBefore(ÅPNINGSTIDSPUNKT) -> dato.atTime(ÅPNINGSTIDSPUNKT)
            else -> nesteÅpneDag(dato).atTime(ÅPNINGSTIDSPUNKT)
        }
    }

    fun nesteÅpneDag(etter: LocalDate): LocalDate = generateSequence(etter.plusDays(1)) { it.plusDays(1) }.first { it.erÅpenDag() }

    private fun erÅpent(tidspunkt: LocalDateTime): Boolean {
        val klokkeslett = tidspunkt.toLocalTime()
        return tidspunkt.toLocalDate().erÅpenDag() && !klokkeslett.isBefore(ÅPNINGSTIDSPUNKT) && klokkeslett.isBefore(STENGETIDSPUNKT)
    }

    private fun LocalDate.erÅpenDag(): Boolean = erHverdag() && !erFastHelligdag()

    private val ÅPNINGSTIDSPUNKT: LocalTime = LocalTime.of(6, 10)
    private val STENGETIDSPUNKT: LocalTime = LocalTime.of(20, 50)
}
