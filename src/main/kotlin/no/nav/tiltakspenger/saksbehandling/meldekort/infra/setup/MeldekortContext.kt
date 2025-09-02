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
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AutomatiskMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.AvbrytMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.LeggTilbakeMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendTilMeldekortApiService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.OvertaMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortVedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Åpen så den kan overstyres i test
 */
open class MeldekortContext(
    sessionFactory: SessionFactory,
    sakService: SakService,
    meldekortVedtakRepo: MeldekortVedtakRepo,
    texasClient: TexasClient,
    navkontorService: NavkontorService,
    oppgaveKlient: OppgaveKlient,
    sakRepo: SakRepo,
    clock: Clock,
    simulerService: SimulerService,
    personKlient: PersonKlient,
    statistikkMeldekortRepo: StatistikkMeldekortRepo,
) {
    open val meldekortBehandlingRepo: MeldekortBehandlingRepo by lazy {
        MeldekortBehandlingPostgresRepo(
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

    val iverksettMeldekortService by lazy {
        IverksettMeldekortService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            sessionFactory = sessionFactory,
            sakService = sakService,
            meldekortVedtakRepo = meldekortVedtakRepo,
            utbetalingRepo = utbetalingRepo,
            clock = clock,
            statistikkMeldekortRepo = statistikkMeldekortRepo,
        )
    }
    val oppdaterMeldekortService by lazy {
        OppdaterMeldekortService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            sakService = sakService,
            simulerService = simulerService,
        )
    }
    val opprettMeldekortBehandlingService by lazy {
        OpprettMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            sakService = sakService,
            navkontorService = navkontorService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }
    val automatiskMeldekortBehandlingService by lazy {
        AutomatiskMeldekortBehandlingService(
            brukersMeldekortRepo = brukersMeldekortRepo,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            sakRepo = sakRepo,
            meldekortVedtakRepo = meldekortVedtakRepo,
            navkontorService = navkontorService,
            sessionFactory = sessionFactory,
            clock = clock,
            simulerService = simulerService,
            personKlient = personKlient,
            oppgaveKlient = oppgaveKlient,
            statistikkMeldekortRepo = statistikkMeldekortRepo,
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

    val underkjennMeldekortBehandlingService by lazy {
        UnderkjennMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            clock = clock,
        )
    }

    val overtaMeldekortBehandlingService by lazy {
        OvertaMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }

    val taMeldekortBehandlingService by lazy {
        TaMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }

    val leggTilbakeMeldekortBehandlingService by lazy {
        LeggTilbakeMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }

    val sendMeldekortTilBeslutterService by lazy {
        SendMeldekortTilBeslutterService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            sakService = sakService,
            simulerService = simulerService,
            clock = clock,
        )
    }

    val avbrytMeldekortBehandlingService by lazy {
        AvbrytMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }
}
