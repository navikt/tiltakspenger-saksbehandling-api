package no.nav.tiltakspenger.saksbehandling.dokument

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.saksbehandling.Configuration
import no.nav.tiltakspenger.saksbehandling.distribusjon.DokdistGateway
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokdistHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.JoarkHttpClient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererUtbetalingsvedtakGateway
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.JournalførVedtaksbrevGateway

open class DokumentContext(
    private val entraIdSystemtokenClient: EntraIdSystemtokenClient,
) {
    private val joarkClient by lazy {
        JoarkHttpClient(
            baseUrl = Configuration.joarkUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.joarkScope) },
        )
    }
    open val dokdistGateway: DokdistGateway by lazy {
        DokdistHttpClient(
            baseUrl = Configuration.dokdistUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.dokdistScope) },
        )
    }
    open val journalførMeldekortGateway: JournalførMeldekortGateway by lazy { joarkClient }
    open val journalførVedtaksbrevGateway: JournalførVedtaksbrevGateway by lazy { joarkClient }
    private val pdfgen by lazy {
        PdfgenHttpClient(Configuration.pdfgenUrl)
    }
    open val genererUtbetalingsvedtakGateway: GenererUtbetalingsvedtakGateway by lazy { pdfgen }
    open val genererInnvilgelsesvedtaksbrevGateway: GenererInnvilgelsesvedtaksbrevGateway by lazy { pdfgen }
    open val genererStansvedtaksbrevGateway: GenererStansvedtaksbrevGateway by lazy { pdfgen }
}
