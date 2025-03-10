package no.nav.tiltakspenger.vedtak.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.vedtak.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.vedtak.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Utbetalingsvedtak

interface GenererUtbetalingsvedtakGateway {
    suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksdeltagelser: List<Tiltaksdeltagelse>,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
