package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.felles.PdfA
import no.nav.tiltakspenger.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak

class GenererFakeUtbetalingsvedtakGateway : GenererUtbetalingsvedtakGateway {
    private val response by lazy { PdfOgJson(PdfA("pdf".toByteArray()), "json") }
    override suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltakstype: String,
        tiltaksnavn: String,
        eksternGjennomføringId: String?,
        eksternDeltagelseId: String,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response.right()
    }
}
