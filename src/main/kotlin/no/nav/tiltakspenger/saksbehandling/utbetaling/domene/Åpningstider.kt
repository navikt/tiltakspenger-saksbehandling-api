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
     * Sier om tidspunktet er innenfor økonomisystemets ordinære åpningstid.
     * Tar ikke høyde for bevegelige helligdager, vedlikehold i helger eller åpning utenom ordinær tid.
     * https://helved-docs.intern.dev.nav.no/v3/doc/faq
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

    /**
     * Finner grensen for når en utbetaling må være sendt for at den tidligst kan stå i reskontroen ved [nå].
     * En utbetaling sendt en åpen dag beregnes samme kveld, neste åpne dag er ventedag, og den står tidligst i reskontroen når den åpner dagen etter.
     * En utbetaling sendt etter stengetid eller på en stengt dag regnes som sendt neste åpne dag.
     */
    fun senesteSendetidspunktSomKanStåIReskontroen(nå: LocalDateTime): LocalDateTime {
        return generateSequence(nå.toLocalDate()) { it.minusDays(1) }
            .first { it.erÅpenDag() && nesteÅpneDag(nesteÅpneDag(it)).atTime(ÅPNINGSTIDSPUNKT) <= nå }
            .atTime(STENGETIDSPUNKT)
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
