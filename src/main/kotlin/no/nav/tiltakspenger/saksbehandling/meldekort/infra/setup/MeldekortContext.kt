package no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.http.MeldekortApiHttpClient
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.GenererVedtaksbrevForUtbetalingKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AutomatiskMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AvbrytMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.ForhåndsvisBrevMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.GjenopptaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OvertaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortbehandlingTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendTilMeldekortApiService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SettMeldekortbehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortbehandlingService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Åpen så den kan overstyres i test
 */
open class MeldekortContext(
    sessionFactory: SessionFactory,
    sakService: SakService,
    meldekortvedtakRepo: MeldekortvedtakRepo,
    texasClient: TexasClient,
    navkontorService: NavkontorService,
    oppgaveKlient: OppgaveKlient,
    sakRepo: SakRepo,
    clock: Clock,
    simulerService: SimulerService,
    statistikkService: StatistikkService,
    genererVedtaksbrevForUtbetalingKlient: GenererVedtaksbrevForUtbetalingKlient,
    navIdentClient: NavIdentClient,
) {
    open val meldekortbehandlingRepo: MeldekortbehandlingRepo by lazy {
        MeldekortbehandlingPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy {
        MeldeperiodePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val brukersMeldekortRepo: BrukersMeldekortRepo by lazy {
        BrukersMeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val utbetalingRepo: UtbetalingRepo by lazy {
        UtbetalingPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    val iverksettMeldekortbehandlingService by lazy {
        IverksettMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            sessionFactory = sessionFactory,
            sakService = sakService,
            meldekortvedtakRepo = meldekortvedtakRepo,
            utbetalingRepo = utbetalingRepo,
            clock = clock,
            statistikkService = statistikkService,
        )
    }
    val oppdaterMeldekortbehandlingService by lazy {
        OppdaterMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            simulerService = simulerService,
        )
    }
    val opprettMeldekortbehandlingService by lazy {
        OpprettMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            navkontorService = navkontorService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }
    val automatiskMeldekortbehandlingService by lazy {
        AutomatiskMeldekortbehandlingService(
            brukersMeldekortRepo = brukersMeldekortRepo,
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakRepo = sakRepo,
            meldekortvedtakRepo = meldekortvedtakRepo,
            navkontorService = navkontorService,
            sessionFactory = sessionFactory,
            simulerService = simulerService,
            sakService = sakService,
            oppgaveKlient = oppgaveKlient,
            statistikkService = statistikkService,
        )
    }

    open val meldekortApiHttpClient: MeldekortApiKlient by lazy {
        MeldekortApiHttpClient(
            baseUrl = Configuration.meldekortApiUrl,
            getToken = { texasClient.getSystemToken(Configuration.meldekortApiScope, IdentityProvider.AZUREAD) },
        )
    }
    val sendTilMeldekortApiService by lazy {
        SendTilMeldekortApiService(
            sakRepo = sakRepo,
            meldekortApiHttpClient = meldekortApiHttpClient,
        )
    }

    val underkjennMeldekortbehandlingService by lazy {
        UnderkjennMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            clock = clock,
            sakService = sakService,
        )
    }

    val overtaMeldekortbehandlingService by lazy {
        OvertaMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            clock = clock,
        )
    }

    val taMeldekortbehandlingService by lazy {
        TaMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            clock = clock,
        )
    }

    val leggTilbakeMeldekortbehandlingService by lazy {
        LeggTilbakeMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            clock = clock,
        )
    }

    val sendMeldekortbehandlingTilBeslutterService by lazy {
        SendMeldekortbehandlingTilBeslutterService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
        )
    }

    val avbrytMeldekortbehandlingService by lazy {
        AvbrytMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            clock = clock,
        )
    }

    val settMeldekortbehandlingPåVentService by lazy {
        SettMeldekortbehandlingPåVentService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            clock = clock,
        )
    }

    val gjenopptaMeldekortbehandlingService by lazy {
        GjenopptaMeldekortbehandlingService(
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            sakService = sakService,
            clock = clock,
        )
    }

    val forhåndsvisBrevMeldekortbehandlingService by lazy {
        ForhåndsvisBrevMeldekortbehandlingService(
            genererBrevClient = genererVedtaksbrevForUtbetalingKlient,
            sakService = sakService,
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            navIdentClient = navIdentClient,
            clock = clock,
        )
    }
}
