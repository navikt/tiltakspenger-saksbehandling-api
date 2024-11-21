package no.nav.tiltakspenger.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.ports.KunneIkkeGenererePdf
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak

interface GenererUtbetalingsvedtakGateway {
    suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksnavn: String,
        eksternGjennomføringId: String?,
        eksternDeltagelseId: String,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
