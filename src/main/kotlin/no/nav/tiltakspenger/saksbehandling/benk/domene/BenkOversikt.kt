package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/**
 * Radene i én fane, sammen med tellingene benken viser over tabellen.
 *
 * [totalAntall] er antallet som matcher filteret, altså før pagineringen kutter.
 * [totalAntallUfiltrert] er antallet i fanen uten filter, slik at benken kan si hvor mange filteret tok bort.
 *
 * [saksbehandlere] og [besluttere] er identene som er tildelt en rad i fanen, uten filter — nedtrekkslisten i benken viser dem som filtervalg.
 */
data class BenkOversikt<out T : BenkBehandling>(
    val behandlinger: List<T>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val saksbehandlere: List<String>,
    val besluttere: List<String>,
) {
    fun fødselsnummere(): List<Fnr> = behandlinger.map { it.fnr }.distinct().sortedBy { it.verdi }

    /**
     * Beholder radene som gjelder [personer], og teller [totalAntall] på nytt.
     * Gjelder bare oversikter der alle radene er hentet, som seksjonene i mine-fanen.
     */
    fun avgrensTil(personer: Set<Fnr>): BenkOversikt<T> {
        require(totalAntall == behandlinger.size) { "Kan bare avgrense en oversikt der alle radene er hentet, men fikk ${behandlinger.size} av $totalAntall" }
        val rader = behandlinger.filter { it.fnr in personer }
        return BenkOversikt(
            behandlinger = rader,
            totalAntall = rader.size,
            totalAntallUfiltrert = totalAntallUfiltrert,
            saksbehandlere = saksbehandlere,
            besluttere = besluttere,
        )
    }
}

/**
 * Antall åpne rader per fane, uten filter.
 * Benken viser dette i fanetitlene, slik at saksbehandler ser hvor arbeidet ligger uten å bytte fane.
 */
data class BenkAntallPerFane(
    val søknader: Int,
    val revurderinger: Int,
    val meldekort: Int,
    val klage: Int,
    val tilbakekreving: Int,
    /** Antallet er per innlogget saksbehandler, til forskjell fra de andre fanenes: behandlingene hen er tildelt som saksbehandler eller beslutter. */
    val mine: Int,
)
