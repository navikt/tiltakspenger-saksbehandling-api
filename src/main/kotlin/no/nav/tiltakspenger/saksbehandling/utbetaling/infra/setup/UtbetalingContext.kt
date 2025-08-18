package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingHttpKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.JournalførUtbetalingsvedtakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.OppdaterUtbetalingsstatusService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SendUtbetalingerService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

open class UtbetalingContext(
    sessionFactory: SessionFactory,
    genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient,
    journalførMeldekortKlient: JournalførMeldekortKlient,
    navIdentClient: NavIdentClient,
    texasClient: TexasClient,
    sakRepo: SakRepo,
    clock: Clock,
    navkontorService: NavkontorService,
) {
    open val utbetalingsklient: Utbetalingsklient by lazy {
        UtbetalingHttpKlient(
            baseUrl = Configuration.utbetalingUrl,
            getToken = { texasClient.getSystemToken(Configuration.utbetalingScope, IdentityProvider.AZUREAD) },
        )
    }
    open val utbetalingsvedtakRepo: UtbetalingsvedtakRepo by lazy {
        UtbetalingsvedtakPostgresRepo(
            sessionFactory as PostgresSessionFactory,
        )
    }
    open val simulerService: SimulerService by lazy {
        SimulerService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            navkontorService = navkontorService,
            utbetalingsklient = utbetalingsklient,
        )
    }
    val sendUtbetalingerService: SendUtbetalingerService by lazy {
        SendUtbetalingerService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            utbetalingsklient = utbetalingsklient,
            clock = clock,
        )
    }
    val journalførUtbetalingsvedtakService by lazy {
        JournalførUtbetalingsvedtakService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            journalførMeldekortKlient = journalførMeldekortKlient,
            genererVedtaksbrevForUtbetalingKlient = genererVedtaksbrevForUtbetalingKlient,
            navIdentClient = navIdentClient,
            sakRepo = sakRepo,
            clock = clock,
        )
    }

    val oppdaterUtbetalingsstatusService by lazy {
        OppdaterUtbetalingsstatusService(
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            utbetalingsklient = utbetalingsklient,
            clock = clock,
        )
    }
}
