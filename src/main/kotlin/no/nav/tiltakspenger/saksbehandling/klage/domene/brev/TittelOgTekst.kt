package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import no.nav.tiltakspenger.libs.common.NonBlankString

data class TittelOgTekst(
    val tittel: NonBlankString,
    val tekst: NonBlankString,
) {
    companion object {
        operator fun invoke(tittel: String, tekst: String) = TittelOgTekst(
            tittel = NonBlankString.create(tittel),
            tekst = NonBlankString.create(tekst),
        )
    }
}
