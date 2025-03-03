package no.nav.tiltakspenger.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak

interface GenererUtbetalingsvedtakGateway {
    suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltakstype: String,
        tiltaksnavn: String,
        eksternGjennomføringId: String?,
        eksternDeltagelseId: String,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
