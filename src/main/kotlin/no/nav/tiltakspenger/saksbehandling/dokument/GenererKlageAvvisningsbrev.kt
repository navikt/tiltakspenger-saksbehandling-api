package no.nav.tiltakspenger.saksbehandling.dokument

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster

typealias GenererKlageAvvisningsbrev = suspend (
    saksnummer: Saksnummer,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    tilleggstekst: Brevtekster,
    forhåndsvisning: Boolean,
) -> Either<KunneIkkeGenererePdf, PdfOgJson>
