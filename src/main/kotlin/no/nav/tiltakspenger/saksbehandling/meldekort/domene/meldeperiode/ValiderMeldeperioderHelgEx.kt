package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.felles.erHverdag
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.meldeperioderErGyldigeForHelg(behandlingId: RammebehandlingId, clock: Clock): Boolean {
    if (kanSendeInnHelgForMeldekort) {
        return true
    }

    val rammebehandling = hentRammebehandling(behandlingId)!!

    // Uten en vedtaksperiode (f.eks. når resultatet er "ikke valgt") finnes det ingen nye meldeperioder å validere.
    if (rammebehandling.vedtaksperiode == null) {
        return meldeperioderErGyldigeForHelg()
    }

    return genererMeldeperioderForValidering(rammebehandling, clock)
        .getOrElse { throw IllegalStateException("Kunne ikke generere meldeperioder for validering på sak ${this.id}: $it") }
        .hentMeldeperiodeKjederMedKunRettIHelg().isEmpty()
}

fun Sak.meldeperioderErGyldigeForHelg(): Boolean {
    return kanSendeInnHelgForMeldekort || meldeperiodeKjeder
        .sisteMeldeperiodePerKjede
        .hentMeldeperiodeKjederMedKunRettIHelg().isEmpty()
}

fun Sak.hentMeldeperiodeKjederMedKunRettIHelg(): List<MeldeperiodeKjedeId> {
    return this.meldeperiodeKjeder
        .sisteMeldeperiodePerKjede
        .hentMeldeperiodeKjederMedKunRettIHelg()
}

private fun List<Meldeperiode>.hentMeldeperiodeKjederMedKunRettIHelg(): List<MeldeperiodeKjedeId> {
    return this.mapNotNull { meldeperiode ->
        val harRettPåMinstEnHverdag by lazy {
            meldeperiode.periode.datoer.any { it.erHverdag() && meldeperiode.girRett[it] == true }
        }

        if (meldeperiode.ingenDagerGirRett || harRettPåMinstEnHverdag) {
            return@mapNotNull null
        }

        meldeperiode.kjedeId
    }
}
