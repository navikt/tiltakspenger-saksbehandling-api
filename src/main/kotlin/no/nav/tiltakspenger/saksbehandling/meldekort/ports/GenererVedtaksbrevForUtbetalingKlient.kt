package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortVedtakBrevKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak

interface GenererVedtaksbrevForUtbetalingKlient {
    suspend fun genererMeldekortvedtakBrev(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentSaksbehandlersNavn: suspend (String) -> String,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
        forhåndsvisning: Boolean,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererMeldekortvedtakBrev(
        kommando: GenererMeldekortVedtakBrevKommando,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
