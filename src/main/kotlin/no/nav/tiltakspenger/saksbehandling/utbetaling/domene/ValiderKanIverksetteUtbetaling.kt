package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

fun Meldekortbehandling.validerKanIverksetteUtbetaling(): Either<KanIkkeIverksetteUtbetaling, Unit> {
    // Ingenting å validere dersom det ikke finnes beregning
    // Beregning er non-nullable for de tilstandene der den er påkrevd
    if (beregning == null) {
        return Unit.right()
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
            if (harJusteringPåTversAvMeldeperioderEllerMåneder()) {
                KanIkkeIverksetteUtbetaling.JusteringStøttesIkke.left()
            } else {
                Unit.right()
            }
        }

        is Simulering.IngenEndring -> Unit.right()

        null -> KanIkkeIverksetteUtbetaling.SimuleringMangler.left()
    }
}

// Vi har ikke lov til å justere utbetalinger på tvers av meldeperioder
private fun Simulering.Endring.harJusteringPåTversAvMeldeperioderEllerMåneder(): Boolean {
    return simuleringPerMeldeperiode.any { meldeperiode ->
        /*
          Dersom meldeperioden går over to måneder, må vi sjekke dagene på hver side av månedsskiftet separat
          Dette ettersom oppdrag kun justerer innenfor samme kalendermåned.
          På tvers av måneder blir det feilutbetaling + etterbetaling for hver måned istedenfor justering
         */
        meldeperiode.harJustering && meldeperiode.simuleringsdager
            .groupBy { it.dato.month }.values
            .any { dagerForMåned ->
                dagerForMåned.sumOf { it.totalJustering } != 0
            }
    }
}

sealed interface KanIkkeIverksetteUtbetaling {
    data object SimuleringMangler : KanIkkeIverksetteUtbetaling

    data object JusteringStøttesIkke : KanIkkeIverksetteUtbetaling

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
        KanIkkeIverksetteUtbetaling.JusteringStøttesIkke,
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeFeilutbetaling,
        KanIkkeIverksetteUtbetaling.BehandlingstypeStøtterIkkeJustering,
        -> logger.warn(meldingMedUtfall)
    }
}
