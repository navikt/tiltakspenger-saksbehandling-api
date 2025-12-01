package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortVedtakBrevCommand
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak

interface GenererVedtaksbrevForUtbetalingKlient {
    suspend fun genererMeldekortvedtakBrev(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererMeldekortvedtakBrev(
        command: GenererMeldekortVedtakBrevCommand,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
