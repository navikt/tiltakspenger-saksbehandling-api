package no.nav.tiltakspenger.saksbehandling.dokument

import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst

data class TittelOgTekstDTO(
    val tittel: String,
    val tekst: String,
)

fun List<TittelOgTekst>.toDTO(): List<TittelOgTekstDTO> {
    return this.map { it.toDto() }
}

fun TittelOgTekst.toDto(): TittelOgTekstDTO {
    return TittelOgTekstDTO(
        tittel = this.tittel.value,
        tekst = this.tekst.value,
    )
}
