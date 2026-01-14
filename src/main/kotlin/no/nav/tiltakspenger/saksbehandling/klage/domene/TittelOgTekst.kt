package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.NonBlankString

data class TittelOgTekst(
    val tittel: NonBlankString,
    val tekst: NonBlankString,
)
