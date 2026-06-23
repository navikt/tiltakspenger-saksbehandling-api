package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilUtfyltMeldeperiode
import java.time.LocalDate

data class Meldeperiodebehandling(
    val dager: UtfyltMeldeperiode,
    /** Foreløpig er kun automatiske behandlinger eksplisitt tilknyttet et brukers meldekort */
    val brukersMeldekort: BrukersMeldekort?,
    val type: MeldeperiodebehandlingType,
    val meldekortbehandlingId: MeldekortId,
) {
    val meldeperiode: Meldeperiode = dager.meldeperiode

    val meldeperiodeId: MeldeperiodeId = meldeperiode.id
    val kjedeId: MeldeperiodeKjedeId = meldeperiode.kjedeId

    val periode: Periode = meldeperiode.periode

    val fraOgMed: LocalDate = periode.fraOgMed
    val tilOgMed: LocalDate = periode.tilOgMed

    val erFullstendigUtfylt: Boolean by lazy { dager.erFullstendigUtfylt }

    init {
        if (brukersMeldekort != null) {
            require(kjedeId == brukersMeldekort.kjedeId) {
                "Brukers meldekort må tilhøre samme meldeperiodekjede som behandlingen - forventet $kjedeId, fant ${brukersMeldekort.kjedeId}"
            }
        }
    }
}

fun Meldeperiode.tilMeldeperiodebehandling(
    type: MeldeperiodebehandlingType,
    meldekortbehandlingId: MeldekortId,
): Meldeperiodebehandling {
    return Meldeperiodebehandling(
        dager = this.tilUtfyltMeldeperiode(),
        brukersMeldekort = null,
        type = type,
        meldekortbehandlingId = meldekortbehandlingId,
    )
}

fun BrukersMeldekort.tilMeldeperiodebehandling(
    type: MeldeperiodebehandlingType,
    meldekortbehandlingId: MeldekortId,
): Meldeperiodebehandling {
    return Meldeperiodebehandling(
        dager = this.tilUtfyltMeldeperiode(),
        brukersMeldekort = this,
        type = type,
        meldekortbehandlingId = meldekortbehandlingId,
    )
}
