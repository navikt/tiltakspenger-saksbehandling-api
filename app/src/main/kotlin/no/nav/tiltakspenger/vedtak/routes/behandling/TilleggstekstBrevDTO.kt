package no.nav.tiltakspenger.vedtak.routes.behandling

import no.nav.tiltakspenger.saksbehandling.domene.behandling.TilleggstekstBrev

data class TilleggstekstBrevDTO(
    val subsumsjon: SubsumsjonDTO,
    val tekst: String,
)

fun TilleggstekstBrev.toDTO(): TilleggstekstBrevDTO =
    TilleggstekstBrevDTO(
        subsumsjon = subsumsjon.toDTO(),
        tekst = tekst,
    )
