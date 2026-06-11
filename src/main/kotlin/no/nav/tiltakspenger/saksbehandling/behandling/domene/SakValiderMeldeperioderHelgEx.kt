package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.felles.erHverdag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.genererMeldeperioderForValidering
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.harGyldigeMeldeperioderForHelg(behandlingId: RammebehandlingId, clock: Clock): Boolean {
    if (kanSendeInnHelgForMeldekort) {
        return true
    }

    val rammebehandling = hentRammebehandling(behandlingId)!!

    // Uten en vedtaksperiode (f.eks. når resultatet er "ikke valgt") finnes det ingen meldeperioder å validere.
    if (rammebehandling.vedtaksperiode == null) {
        return harGyldigeMeldeperioderForHelg()
    }

    return genererMeldeperioderForValidering(rammebehandling, clock)
        .hentMeldeperiodeKjederMedKunRettIHelg().isEmpty()
}

fun Sak.harGyldigeMeldeperioderForHelg(): Boolean {
    return kanSendeInnHelgForMeldekort || meldeperiodeKjeder
        .sisteMeldeperiodePerKjede
        .hentMeldeperiodeKjederMedKunRettIHelg().isEmpty()
}

fun Sak.hentMeldeperiodeKjederMedKunRettIHelg(): List<MeldeperiodeKjedeId> {
    return this.meldeperiodeKjeder.sisteMeldeperiodePerKjede.hentMeldeperiodeKjederMedKunRettIHelg()
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
