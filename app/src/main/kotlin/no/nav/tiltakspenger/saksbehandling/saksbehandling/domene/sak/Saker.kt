package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak

import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Inneholder en liste over alle saker som er tilgjengelig for en bruker/ident.
 * Garanterer at sakid og  saksnummer er unikt i listen.
 */
data class Saker(
    val fnr: Fnr,
    val saker: List<Sak>,
) : List<Sak> by saker {
    init {
        saker.map { it.saksnummer }.also {
            require(it.size == it.toSet().size) { "Saker inneholder duplikate saksnummer: $it" }
        }
        saker.map { it.id }.also {
            require(it.size == it.toSet().size) { "Saker inneholder duplikate IDer: $it" }
        }
        saker.map { it.fnr }.also {
            require(it.toSet().size <= 1) { "Oppdaget flere enn 1 ident. En brukers saker må være knyttet til samme ident." }
        }
        saker.flatMap { sak -> sak.behandlinger.map { it.id } }.also {
            require(it.size == it.toSet().size) { "Saker inneholder duplikate behandlingsid'er: $it" }
        }
    }

    fun hentForSaksnummer(saksnummer: Saksnummer): Sak? = saker.find { it.saksnummer == saksnummer }
}
