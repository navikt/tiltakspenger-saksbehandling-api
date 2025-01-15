package no.nav.tiltakspenger.vedtak.routes.behandling

import no.nav.tiltakspenger.saksbehandling.domene.behandling.TilleggstekstBrev

enum class SubsumsjonDTO {
    TILTAKSDELTAGELSE,
}

internal fun TilleggstekstBrev.Subsumsjon.toDTO(): SubsumsjonDTO {
    return when (this) {
        TilleggstekstBrev.Subsumsjon.TILTAKSDELTAGELSE -> SubsumsjonDTO.TILTAKSDELTAGELSE
    }
}
