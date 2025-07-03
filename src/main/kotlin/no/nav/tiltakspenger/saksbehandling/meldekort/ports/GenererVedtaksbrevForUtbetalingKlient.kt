package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak

interface GenererVedtaksbrevForUtbetalingKlient {
    suspend fun genererUtbetalingsvedtak(
        utbetalingsvedtak: Utbetalingsvedtak,
        tiltaksdeltagelser: List<Tiltaksdeltagelse>,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
