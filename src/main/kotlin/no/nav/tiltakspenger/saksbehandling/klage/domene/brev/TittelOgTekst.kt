package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import no.nav.tiltakspenger.libs.common.NonBlankString

data class TittelOgTekst(
    val tittel: NonBlankString,
    val tekst: NonBlankString,
)
