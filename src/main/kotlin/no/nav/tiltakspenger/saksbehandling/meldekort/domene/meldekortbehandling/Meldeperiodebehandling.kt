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
    /**
     * Meldekortene fra bruker som denne meldeperiodebehandlingen behandler, sortert eldst først.
     * Tom liste betyr at behandlingen ikke er knyttet til noe meldekort fra bruker.
     */
    val brukersMeldekort: List<BrukersMeldekort>,
    val type: MeldeperiodebehandlingType,
    val meldekortbehandlingId: MeldekortId,
) {
    val meldeperiode: Meldeperiode = dager.meldeperiode

    val meldeperiodeId: MeldeperiodeId = meldeperiode.id
    val kjedeId: MeldeperiodeKjedeId = meldeperiode.kjedeId

    val periode: Periode = meldeperiode.periode

    val fraOgMed: LocalDate = periode.fraOgMed
    val tilOgMed: LocalDate = periode.tilOgMed

    val erKorrigering: Boolean = type == MeldeperiodebehandlingType.KORRIGERING

    val erFullstendigUtfylt: Boolean by lazy { dager.erFullstendigUtfylt }

    init {
        brukersMeldekort.forEach {
            require(kjedeId == it.kjedeId) {
                "Brukers meldekort må tilhøre samme meldeperiodekjede som behandlingen - forventet $kjedeId, fant ${it.kjedeId}"
            }
        }
        require(brukersMeldekort.map { it.id }.distinct().size == brukersMeldekort.size) {
            "Samme meldekort fra bruker kan ikke behandles flere ganger i én meldeperiodebehandling - fant ${brukersMeldekort.map { it.id }}"
        }
    }
}

fun Meldeperiode.tilMeldeperiodebehandling(
    type: MeldeperiodebehandlingType,
    meldekortbehandlingId: MeldekortId,
): Meldeperiodebehandling {
    return Meldeperiodebehandling(
        dager = this.tilUtfyltMeldeperiode(),
        brukersMeldekort = emptyList(),
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
        brukersMeldekort = listOf(this),
        type = type,
        meldekortbehandlingId = meldekortbehandlingId,
    )
}
