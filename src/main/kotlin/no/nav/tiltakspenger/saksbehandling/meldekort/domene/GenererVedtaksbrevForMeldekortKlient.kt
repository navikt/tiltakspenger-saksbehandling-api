package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.SammenligningAvBeregninger
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.dokument.infra.GenererMeldekortvedtakBrevKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.person.Navn
import java.time.LocalDate

interface GenererVedtaksbrevForMeldekortKlient {
    suspend fun genererMeldekortvedtakBrev(
        meldekortvedtak: Meldekortvedtak,
        tiltaksdeltakelser: Tiltaksdeltakelser,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
        sammenligning: (MeldeperiodeBeregning) -> SammenligningAvBeregninger.MeldeperiodeSammenligninger,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererMeldekortvedtakBrev(
        kommando: GenererMeldekortvedtakBrevKommando,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        vedtaksdato: LocalDate,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
