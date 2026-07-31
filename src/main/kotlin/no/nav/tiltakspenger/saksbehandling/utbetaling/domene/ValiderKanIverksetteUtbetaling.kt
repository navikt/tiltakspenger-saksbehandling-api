package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun Meldekortbehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    // Ingenting å validere dersom det ikke finnes beregning
    // Beregning er non-nullable for de tilstandene der den er påkrevd
    if (beregning == null) {
        return Unit.right()
    }

    // Kontrollen kjøres først når behandlingen sendes videre i flyten, så fram til da er det ingenting å sammenligne mot.
    // Mangler simuleringen på behandlingen, er det [KanIkkeIverksetteUtbetaling.SimuleringMangler] som er den dekkende feilen, og den håndteres lenger ned.
    val kontrollsimulering = this.utbetalingskontroll?.simulering
    if (simulering != null && kontrollsimulering != null) {
        simulering.finnUlikheter(kontrollsimulering).toNonEmptyListOrNull()?.let {
            return KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer(it).left()
        }
    }

    if (status == MeldekortbehandlingStatus.AUTOMATISK_BEHANDLET) {
        if (simulering?.harJustering == true) {
            return KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering.left()
        }

        if (simulering?.harFeilutbetaling == true) {
            return KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling.left()
        }
    }

    return simulering.validerKanIverksetteUtbetaling()
}

fun Rammebehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    val simulering = this.utbetaling?.simulering
    val kontrollSimulering = this.utbetalingskontroll?.simulering

    simulering.finnUlikheter(kontrollSimulering).toNonEmptyListOrNull()?.let {
        return KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer(it).left()
    }

    // Hvis beregnet utbetaling er null (og kontrollen også var null), er alt ok
    if (this.utbetaling == null) {
        return Unit.right()
    }

    if (simulering == null) {
        return KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }

    if (this.resultat !is Omgjøringsresultat) {
        if (simulering.harFeilutbetaling) {
            return KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling.left()
        }
        if (simulering.harJustering) {
            return KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering.left()
        }
    }

    return simulering.validerKanIverksetteUtbetaling()
}

fun Simulering?.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    return when (this) {
        is Simulering.Endring -> {
            val ubalanserte = finnUbalanserteJusteringer()
            when {
                ubalanserte == null -> Unit.right()

                /*
                  Oppdrag omfordeler forskuddstrekk mellom utbetalingsperioder, med justeringer som regnskapsmessig motpost.
                  Trekket beregnes per kalendermåned, så justeringene krysser meldeperiodegrensene -- men summerer til null innenfor måneden, og månedens utbetaling til bruker er uendret.
                  Sett i dev 2026-07-24 på rene førstegangsutbetalinger uten korrigering (`TrekkMedJusteringFraDevTest`).
                  Å sperre der stopper saken uten grunn.

                  Tre vilkår må alle holde for å tillate, og da får saksbehandler advarsel i visningen i stedet for sperre:
                  månedsbalansen må være i behold, det kan ikke være feilutbetaling (motregnings-/kravgrunnlagsklassen), og ingen av de ubalanserte meldeperiodene kan ha reversert ytelse.
                  Det siste skiller trekkomfordeling fra flytting av ytelse: korrigeres en utbetalt dag bort i én meldeperiode og motregnes mot en økning i en annen, balanserer justeringene i måneden uten feilutbetaling -- men da er det selve ytelsen som er flyttet mellom meldeperioder, og det har vi ikke hjemmel til.
                 */
                ubalanserteJusteringsmåneder.isEmpty() &&
                    !harFeilutbetaling &&
                    simuleringPerMeldeperiode.none { it.harUbalansertJustering && it.harReversertYtelse }
                -> Unit.right()

                else -> KanIkkeIverksetteUtbetaling.JusteringStøttesIkke(ubalanserte).left()
            }
        }

        is Simulering.IngenEndring -> Unit.right()

        null -> KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }
}

/**
 * Vi har ikke lov til å justere utbetalinger på tvers av meldeperioder.
 *
 * Dersom meldeperioden går over to måneder, må vi vurdere hver side av månedsskiftet for seg.
 * Det er fordi oppdrag kun justerer innenfor samme kalendermåned.
 * På tvers av måneder blir det feilutbetaling + etterbetaling for hver måned i stedet for justering.
 *
 * Selve summeringen ligger på [SimuleringForMeldeperiode.ubalanserteJusteringsmåneder], slik at vernet og flagget vi viser saksbehandler ikke kan drive fra hverandre.
 */
private fun Simulering.Endring.finnUbalanserteJusteringer(): NonEmptyList<KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.UbalansertJustering>? {
    return simuleringPerMeldeperiode
        .filter { it.harUbalansertJustering }
        .map {
            KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.UbalansertJustering(
                meldeperiode = it.meldeperiode.periode,
                beløpPerMåned = it.ubalanserteJusteringsmåneder,
            )
        }
        .toNonEmptyListOrNull()
}

sealed interface KanIkkeIverksetteUtbetaling {
    data object SimuleringMangler : KanIkkeIverksetteUtbetaling

    /**
     * Justeringen i simuleringen balanserer ikke innenfor meldeperioden og kalendermåneden.
     * [ubalanserte] sier hvilke meldeperioder og hvor mye, slik at saksbehandler får se hvor beløpet er flyttet i stedet for bare at noe «ikke støttes».
     */
    data class JusteringStøttesIkke(
        val ubalanserte: NonEmptyList<UbalansertJustering>,
    ) : KanIkkeIverksetteUtbetaling {

        data class UbalansertJustering(
            val meldeperiode: Periode,
            /** Summen av justeringsposteringene per kalendermåned, kun for månedene som ikke går opp i null. */
            val beløpPerMåned: Map<YearMonth, Int>,
        ) {
            fun beskriv(): String {
                // Sortert på måned, slik at meldingen er stabil uansett hvordan map-et ble bygget.
                val måneder = beløpPerMåned.entries.sortedBy { it.key }.joinToString { (måned, beløp) ->
                    "${formaterBeløpMedFortegn(beløp)} i ${måned.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("nb"))}"
                }
                return "${meldeperiode.fraOgMed.format(kortDatoFormat)}–${meldeperiode.tilOgMed.format(kortDatoFormat)} ($måneder)"
            }
        }

        /**
         * PII-fri melding som kan vises til saksbehandler.
         * Sier hvilke meldeperioder som ikke balanserer og med hvor mye, slik at det går an å forstå hva oppdragssystemet har gjort.
         */
        val beskrivelse: String
            get() =
                "Justeringen i simuleringen balanserer ikke innenfor meldeperioden og kalendermåneden: ${ubalanserte.joinToString { it.beskriv() }}. " +
                    "Oppdragssystemet beregner per kalendermåned og har motregnet beløp på tvers av meldeperioder. Det har vi ikke hjemmel til, så utbetalingen kan ikke iverksettes."

        override fun toString() = "JusteringStøttesIkke(${ubalanserte.joinToString { it.beskriv() }})"
    }

    data object BehandlingstypeStøtterIkkeFeilutbetaling : KanIkkeIverksetteUtbetaling

    data object BehandlingstypeStøtterIkkeJustering : KanIkkeIverksetteUtbetaling

    /**
     * Kontrollsimuleringen avviker fra simuleringen på beregningen.
     * Skjer typisk når en annen utbetaling på saken har blitt iverksatt eller effektuert mellom send til beslutter og iverksett.
     * [ulikheter] beskriver hva som avviker, slik at én logglinje er nok til å se hva som faktisk endret seg.
     */
    data class KontrollSimuleringHarEndringer(val ulikheter: NonEmptyList<String>) : KanIkkeIverksetteUtbetaling {
        override fun toString() = "KontrollSimuleringHarEndringer(${ulikheter.joinToString("; ")})"
    }
}

/**
 * [KanIkkeIverksetteUtbetaling.SimuleringMangler] tyder på feil hos oss og logges som error.
 * De øvrige utfallene er forventede domeneutfall som saksbehandler får presentert og kan handle på, og logges derfor som warn.
 * Linjen stemples alltid med `KanIkkeIverksetteUtbetaling.<utfall>`, slik at man kan søke på `KanIkkeIverksetteUtbetaling` for alle utfall og på f.eks. `KontrollSimuleringHarEndringer` for ett spesifikt.
 */
fun KanIkkeIverksetteUtbetaling.logg(logger: KLogger, melding: () -> Any?) {
    val meldingMedUtfall = { "${melding()} - KanIkkeIverksetteUtbetaling.$this" }

    when (this) {
        KanIkkeIverksetteUtbetaling.SimuleringMangler -> logger.error(meldingMedUtfall)

        is KanIkkeIverksetteUtbetaling.KontrollSimuleringHarEndringer,
        is KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling,
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
        -> logger.warn(meldingMedUtfall)
    }
}

private val kortDatoFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** «+715 kr» / «−440 kr» -- fortegnet er poenget når et beløp er flyttet mellom meldeperioder. */
private fun formaterBeløpMedFortegn(beløp: Int): String {
    return if (beløp < 0) "−${-beløp} kr" else "+$beløp kr"
}
