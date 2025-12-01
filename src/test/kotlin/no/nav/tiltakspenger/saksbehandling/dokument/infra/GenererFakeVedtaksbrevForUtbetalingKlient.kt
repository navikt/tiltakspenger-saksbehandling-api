package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient

class GenererFakeVedtaksbrevForUtbetalingKlient : GenererVedtaksbrevForUtbetalingKlient {
    private val response by lazy { PdfOgJson(PdfA("pdf".toByteArray()), "json") }
    override suspend fun genererMeldekortvedtakBrev(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response.right()
    }

    override suspend fun genererMeldekortvedtakBrev(
        command: GenererMeldekortVedtakBrevCommand,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return response.right()
    }
}
