package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.Fnr

data class BenkOversikt(
    val behandlingssammendrag: List<Behandlingssammendrag>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
) {

    fun isEmpty(): Boolean = behandlingssammendrag.isEmpty() && totalAntall == 0

    fun filtrerOversikt(
        fn: (Behandlingssammendrag) -> Boolean,
    ): BenkOversikt = this.copy(behandlingssammendrag = this.behandlingssammendrag.filter(fn))

    fun fødselsnummere(): List<Fnr> = behandlingssammendrag.map { it.fnr }.distinct().sortedBy { it.verdi }
}
