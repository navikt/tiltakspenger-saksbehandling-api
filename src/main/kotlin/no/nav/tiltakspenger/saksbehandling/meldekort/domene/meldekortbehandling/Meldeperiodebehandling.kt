package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDager
import java.time.LocalDate

data class Meldeperiodebehandling(
    val dager: UtfyltMeldeperiode,
    /** Pdd har kun automatiske behandlinger tilknyttet et brukers meldekort */
    val brukersMeldekort: BrukersMeldekort?,
) {
    val meldeperiode: Meldeperiode = dager.meldeperiode
    val kjedeId: MeldeperiodeKjedeId = meldeperiode.kjedeId

    val periode: Periode = meldeperiode.periode

    val fraOgMed: LocalDate = periode.fraOgMed
    val tilOgMed: LocalDate = periode.tilOgMed
}

fun Meldeperiode.tilMeldeperiodebehandling(): Meldeperiodebehandling {
    return Meldeperiodebehandling(
        dager = this.tilMeldekortDager(),
        brukersMeldekort = null,
    )
}

fun BrukersMeldekort.tilMeldeperiodebehandling(): Meldeperiodebehandling {
    return Meldeperiodebehandling(
        dager = this.tilUtfyltMeldeperiode(),
        brukersMeldekort = this,
    )
}
