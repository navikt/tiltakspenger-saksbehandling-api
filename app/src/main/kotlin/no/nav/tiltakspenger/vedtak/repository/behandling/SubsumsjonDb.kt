package no.nav.tiltakspenger.vedtak.repository.behandling

import no.nav.tiltakspenger.saksbehandling.domene.behandling.TilleggstekstBrev

enum class SubsumsjonDb {
    TILTAKSDELTAGELSE,
}

fun SubsumsjonDb.toDomain(): TilleggstekstBrev.Subsumsjon =
    when (this) {
        SubsumsjonDb.TILTAKSDELTAGELSE -> TilleggstekstBrev.Subsumsjon.TILTAKSDELTAGELSE
    }

fun TilleggstekstBrev.Subsumsjon.toDb(): SubsumsjonDb =
    when (this) {
        TilleggstekstBrev.Subsumsjon.TILTAKSDELTAGELSE -> SubsumsjonDb.TILTAKSDELTAGELSE
    }
