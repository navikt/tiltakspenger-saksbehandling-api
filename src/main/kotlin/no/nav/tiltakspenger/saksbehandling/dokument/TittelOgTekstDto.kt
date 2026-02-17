package no.nav.tiltakspenger.saksbehandling.dokument

import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst

data class TittelOgTekstDTO(
    val tittel: String,
    val tekst: String,
)

fun List<TittelOgTekst>.toBrevDtoList(): List<TittelOgTekstDTO> =
    this.map { it.toBrevDto() }

fun TittelOgTekst.toBrevDto(): TittelOgTekstDTO =
    TittelOgTekstDTO(
        tittel = this.tittel.value,
        tekst = this.tekst.value,
    )
