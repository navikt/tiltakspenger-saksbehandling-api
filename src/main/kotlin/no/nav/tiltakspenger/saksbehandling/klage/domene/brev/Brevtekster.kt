package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

/**
 * En liste med par av tittel og tekst som skal brukes i et brev.
 * Dersom listen er tom, genereres ingen tekstblokker i brevet.
 */
data class Brevtekster(
    val tekster: List<TittelOgTekst>,
) : List<TittelOgTekst> by tekster {

    companion object {
        val empty = Brevtekster(emptyList())
    }
}
