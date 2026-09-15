package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst

/**
 * Brevtekst ut til saksbehandler, der teksten er saksbehandlers fritekst og kan sladdes.
 * Tittelen er en fast ledetekst og er ikke sladdbar.
 * Skiller seg fra [no.nav.tiltakspenger.saksbehandling.dokument.TittelOgTekstDTO], som går til pdfgen og aldri sladdes.
 */
data class TittelOgSladdbarTekstDTO(
    val tittel: String,
    val tekst: SladdbarVerdi<String>,
)

fun List<TittelOgTekst>.tilTittelOgSladdbarTekstDTO(): List<TittelOgSladdbarTekstDTO> = this.map {
    TittelOgSladdbarTekstDTO(
        tittel = it.tittel.value,
        tekst = it.tekst.value.ikkeSladdet(),
    )
}
