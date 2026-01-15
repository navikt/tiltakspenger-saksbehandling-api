package no.nav.tiltakspenger.saksbehandling.dokument

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

typealias GenererKlageAvvisningsbrev = suspend (
    saksnummer: Saksnummer,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    tilleggstekst: Brevtekster,
    forhåndsvisning: Boolean,

) -> Either<KunneIkkeGenererePdf, PdfOgJson>
