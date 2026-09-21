package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider
import java.time.Clock
import java.time.LocalDateTime

/** Når saken skal slås opp igjen, og hvor mange oppslag på rad som har feilet. */
data class Oppslagsplan(
    val nesteOppslag: LocalDateTime,
    val antallFeilPåRad: Int,
) {
    init {
        require(antallFeilPåRad >= 0) { "Antall feil på rad kan ikke være negativt" }
    }

    companion object {
        /**
         * Legger neste oppslag til neste åpne dag når saken har en nylig sendt utbetaling, ellers 30 dager fram.
         * En utbetaling som ikke står i reskontroen om morgenen, kommer tidligst neste åpne dag, så flere oppslag samme dag gir ikke noe nytt.
         * Intervallene er faste til vi har målinger fra dev.
         */
        fun etterVellykketOppslag(hentet: LocalDateTime, harNyligSendtUtbetaling: Boolean): Oppslagsplan = Oppslagsplan(
            nesteOppslag = Åpningstider.førsteÅpneTidspunkt(
                if (harNyligSendtUtbetaling) hentet.toLocalDate().plusDays(1).atStartOfDay() else hentet.plusDays(30),
            ),
            antallFeilPåRad = 0,
        )

        /**
         * Øker ventetiden med antall feil på rad etter standardtrappa i libs, fra ett minutt opp til ett døgn.
         * Legger forsøket innenfor åpningstiden, fordi tjenesten ikke sier fra om at økonomisystemet er stengt.
         */
        fun etterFeiletOppslag(hentet: LocalDateTime, tidligereFeilPåRad: Int, clock: Clock): Oppslagsplan {
            require(tidligereFeilPåRad >= 0) { "Tidligere feil på rad kan ikke være negativt" }
            val antallFeilPåRad = tidligereFeilPåRad + 1
            val (_, nesteForsøk) = hentet.shouldRetry(count = antallFeilPåRad.toLong(), clock = clock)
            return Oppslagsplan(
                nesteOppslag = Åpningstider.førsteÅpneTidspunkt(nesteForsøk),
                antallFeilPåRad = antallFeilPåRad,
            )
        }
    }
}
