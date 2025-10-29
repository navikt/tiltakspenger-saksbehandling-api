package no.nav.tiltakspenger.saksbehandling.dokument.infra.setup

import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokdistHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivHttpClient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient

open class DokumentContext(
    private val texasClient: TexasClient,
) {
    private val dokarkivClient by lazy {
        DokarkivHttpClient(
            baseUrl = Configuration.dokarkivUrl,
            getToken = { texasClient.getSystemToken(Configuration.dokarkivScope, IdentityProvider.AZUREAD) },
        )
    }
    open val dokumentdistribusjonsklient: Dokumentdistribusjonsklient by lazy {
        DokdistHttpClient(
            baseUrl = Configuration.dokdistUrl,
            getToken = { texasClient.getSystemToken(Configuration.dokdistScope, IdentityProvider.AZUREAD) },
        )
    }
    open val journalførMeldekortKlient: JournalførMeldekortKlient by lazy { dokarkivClient }
    open val journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient by lazy { dokarkivClient }
    private val pdfgen by lazy {
        PdfgenHttpClient(Configuration.pdfgenUrl)
    }
    open val genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient by lazy { pdfgen }
    open val genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient by lazy { pdfgen }
    open val genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient by lazy { pdfgen }
    open val genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient by lazy { pdfgen }
}
