package no.nav.tiltakspenger.saksbehandling.klage.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate

interface GenererKlagebrevKlient {
    suspend fun genererAvvisningsvedtak(
        saksnummer: Saksnummer,
        fnr: Fnr,
        tilleggstekst: Brevtekster,
        saksbehandlerNavIdent: String,
        vedtaksdato: LocalDate,
        forhåndsvisning: Boolean,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererInnstillingsbrev(
        saksnummer: Saksnummer,
        fnr: Fnr,
        tilleggstekst: Brevtekster,
        saksbehandlerNavIdent: String,
        `forhåndsvisning`: Boolean,
        vedtaksdato: LocalDate,
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        innsendingsdato: LocalDate,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
