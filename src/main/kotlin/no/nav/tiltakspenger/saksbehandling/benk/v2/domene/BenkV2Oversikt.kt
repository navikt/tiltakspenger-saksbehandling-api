package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Radene i én fane, sammen med tellingene benken viser over tabellen.
 *
 * [totalAntall] er antallet som matcher filteret, altså før [BenkV2Repo.DEFAULT_LIMIT] kutter.
 * [totalAntallUfiltrert] er antallet i fanen uten filter, slik at benken kan si hvor mange filteret tok bort.
 */
data class BenkV2Oversikt<T : BenkV2Behandling>(
    val behandlinger: List<T>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
) {
    fun isEmpty(): Boolean = behandlinger.isEmpty()

    fun filtrer(fn: (T) -> Boolean): BenkV2Oversikt<T> = this.copy(behandlinger = this.behandlinger.filter(fn))

    fun fødselsnummere(): List<Fnr> = behandlinger.map { it.fnr }.distinct().sortedBy { it.verdi }
}

/**
 * Antall åpne rader per fane, uten filter.
 * Benken viser dette i fanetitlene, slik at saksbehandler ser hvor arbeidet ligger uten å bytte fane.
 */
data class BenkV2AntallPerFane(
    val søknader: Int,
    val revurderinger: Int,
    val meldekort: Int,
    val klage: Int,
    val tilbakekreving: Int,
)
