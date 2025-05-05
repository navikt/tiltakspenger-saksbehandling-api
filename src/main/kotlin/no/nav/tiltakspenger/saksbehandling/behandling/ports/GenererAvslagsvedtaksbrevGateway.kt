package no.nav.tiltakspenger.saksbehandling.behandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

interface GenererAvslagsvedtaksbrevGateway {
    suspend fun genererAvslagsVedtaksbrev(
        hentBrukersNavn: suspend (Fnr) -> Navn,
        hentSaksbehandlersNavn: suspend (String) -> String,
        avslagsgrunner: Set<Avslagsgrunnlag>,
        fnr: Fnr,
        saksbehandlerNavIdent: String,
        beslutterNavIdent: String?,
        avslagsperiode: Periode,
        saksnummer: Saksnummer,
        sakId: SakId,
        tilleggstekst: FritekstTilVedtaksbrev,
        forhåndsvisning: Boolean,
        harSøktBarnetillegg: Boolean,
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
