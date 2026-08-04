package no.nav.tiltakspenger.saksbehandling.dokument.infra.setup

import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForOpphørKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.domene.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.distribusjon.infra.DokdistHttpClient
import no.nav.tiltakspenger.saksbehandling.dokument.infra.PdfgenHttpClient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.journalføring.infra.http.DokarkivHttpClient
import no.nav.tiltakspenger.saksbehandling.klage.domene.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.klage.domene.JournalførKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.GenererVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.JournalførMeldekortKlient
import java.time.Clock

open class DokumentContext(
    private val texasClient: TexasClient,
    private val clock: Clock,
) {
    private val dokarkivClient by lazy {
        DokarkivHttpClient(
            baseUrl = Configuration.dokarkivUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.dokarkivScope,
            ),
        )
    }
    open val dokumentdistribusjonsklient: Dokumentdistribusjonsklient by lazy {
        DokdistHttpClient(
            baseUrl = Configuration.dokdistUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.dokdistScope,
            ),
        )
    }
    open val journalførMeldekortKlient: JournalførMeldekortKlient by lazy { dokarkivClient }
    open val journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient by lazy { dokarkivClient }
    open val journalførKlagevedtaksbrevKlient: JournalførKlagebrevKlient by lazy { dokarkivClient }
    private val pdfgen by lazy {
        PdfgenHttpClient(
            basePdfgenrsUrl = Configuration.pdfgenrsUrl,
            clock = clock,
        )
    }
    open val genererVedtaksbrevForMeldekortKlient: GenererVedtaksbrevForMeldekortKlient by lazy { pdfgen }
    open val genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient by lazy { pdfgen }
    open val genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient by lazy { pdfgen }
    open val genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient by lazy { pdfgen }
    open val genererVedtaksbrevForOpphørKlient: GenererVedtaksbrevForOpphørKlient by lazy { pdfgen }
    open val genererKlagebrevKlient: GenererKlagebrevKlient by lazy { pdfgen }
}
