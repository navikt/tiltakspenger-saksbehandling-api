package no.nav.tiltakspenger.saksbehandling.dokument.infra.setup

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokdistHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.JoarkHttpClient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient

open class DokumentContext(
    private val entraIdSystemtokenClient: EntraIdSystemtokenClient,
) {
    private val joarkClient by lazy {
        JoarkHttpClient(
            baseUrl = Configuration.joarkUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.joarkScope) },
        )
    }
    open val dokumentdistribusjonsklient: Dokumentdistribusjonsklient by lazy {
        DokdistHttpClient(
            baseUrl = Configuration.dokdistUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.dokdistScope) },
        )
    }
    open val journalførMeldekortKlient: JournalførMeldekortKlient by lazy { joarkClient }
    open val journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient by lazy { joarkClient }
    private val pdfgen by lazy {
        PdfgenHttpClient(Configuration.pdfgenUrl)
    }
    open val genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient by lazy { pdfgen }
    open val genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient by lazy { pdfgen }
    open val genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient by lazy { pdfgen }
    open val genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient by lazy { pdfgen }
}
