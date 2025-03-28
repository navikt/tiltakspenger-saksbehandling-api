package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak

interface GenererUtbetalingsvedtakGateway {
    suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksdeltagelser: List<Tiltaksdeltagelse>,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
