package no.nav.tiltakspenger.saksbehandling.common

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import java.util.concurrent.CopyOnWriteArrayList

/**
 * En [KLogger] som samler opp logglinjene i minnet i stedet for å skrive dem ut, slik at tester kan asserte på innholdet i det som logges.
 *
 * Fangeren er bevisst per instans og deler ingen tilstand: den henger ikke på den globale logback-loggeren, slik en `ListAppender` gjør.
 * Tusenvis av tester kan derfor kjøre samtidig – også innenfor samme testfil – uten at logglinjer lekker mellom dem.
 *
 * Alle hjelpemetodene på [KLogger] (`info {}`, `error(t) {}`, `atWarn {}` …) har default-implementasjoner som delegerer til [at], så det holder å implementere de tre abstrakte medlemmene for å fange enhver kallform.
 *
 * Klassen er kopiert fra tiltakspenger-meldekort-api.
 * Den bør løftes til `tiltakspenger-libs:test-common` når flere repoer trenger den, slik at kopiene ikke driver fra hverandre.
 */
class Loggfanger(
    override val name: String,
) : KLogger {

    data class Logglinje(
        val nivå: Level,
        val melding: String?,
        val feil: Throwable?,
    ) {
        /**
         * Meldingene i hele årsakskjeden, ytterst først.
         * Feilen som logges er typisk pakket inn flere ganger (f.eks. `RuntimeException` rundt en `PSQLException`), og det er den innerste som bærer detaljene.
         */
        fun årsakskjede(): String =
            generateSequence(feil) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
    }

    private val linjer = CopyOnWriteArrayList<Logglinje>()

    val logglinjer: List<Logglinje> get() = linjer.toList()

    fun linjerPå(nivå: Level): List<Logglinje> = logglinjer.filter { it.nivå == nivå }

    override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean = true

    override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) {
        val hendelse = KLoggingEventBuilder().apply(block)
        linjer.add(Logglinje(nivå = level, melding = hendelse.message, feil = hendelse.cause))
    }
}

/**
 * En [Sikkerlogg] som samler opp linjene i minnet i stedet for å skrive dem ut, slik at tester kan asserte på det som havner i sikkerloggen.
 *
 * Samme begrunnelse som [Loggfanger]: per instans, ingen delt tilstand, tåler at vilkårlig mange tester kjører samtidig.
 * [Sikkerlogg] er et interface i libs nettopp for å kunne injiseres — companion-objektet er bare default-instansen for kallsteder som ennå ikke gjør det.
 */
class Sikkerloggfanger(
    override val seSikkerlogg: String = "Se sikkerlogg",
) : Sikkerlogg {

    enum class Nivå { TRACE, DEBUG, INFO, WARN, ERROR }

    data class Sikkerlogglinje(
        val nivå: Nivå,
        val melding: String?,
        val feil: Throwable?,
    ) {
        /** Meldingene i hele årsakskjeden, ytterst først. */
        fun årsakskjede(): String =
            generateSequence(feil) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
    }

    private val linjer = CopyOnWriteArrayList<Sikkerlogglinje>()

    val sikkerlogglinjer: List<Sikkerlogglinje> get() = linjer.toList()

    fun linjerPå(nivå: Nivå): List<Sikkerlogglinje> = sikkerlogglinjer.filter { it.nivå == nivå }

    private fun registrer(nivå: Nivå, throwable: Throwable?, loggstatement: () -> Any?) {
        linjer.add(Sikkerlogglinje(nivå = nivå, melding = loggstatement()?.toString(), feil = throwable))
    }

    override fun trace(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.TRACE, throwable, loggstatement)

    override fun debug(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.DEBUG, throwable, loggstatement)

    override fun info(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.INFO, throwable, loggstatement)

    override fun warn(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.WARN, throwable, loggstatement)

    override fun error(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.ERROR, throwable, loggstatement)
}
