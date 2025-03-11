package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.felles.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.felles.PdfA
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak

class GenererFakeUtbetalingsvedtakGateway : GenererUtbetalingsvedtakGateway {
    private val response by lazy { PdfOgJson(PdfA("pdf".toByteArray()), "json") }
    override suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksdeltagelser: List<Tiltaksdeltagelse>,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response.right()
    }
}
