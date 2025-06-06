package no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
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
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppgaveMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendTilMeldekortApiService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.TaMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.OvertaMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

/**
 * Åpen så den kan overstyres i test
 */
@Suppress("unused")
open class MeldekortContext(
    sessionFactory: SessionFactory,
    sakService: SakService,
    tilgangsstyringService: TilgangsstyringService,
    personService: PersonService,
    utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    statistikkStønadRepo: StatistikkStønadRepo,
    entraIdSystemtokenClient: EntraIdSystemtokenClient,
    navkontorService: NavkontorService,
    oppgaveKlient: OppgaveKlient,
    sakRepo: SakRepo,
    clock: Clock,
    simulerService: SimulerService,
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

    val iverksettMeldekortService by lazy {
        IverksettMeldekortService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            brukersMeldekortRepo = brukersMeldekortRepo,
            sessionFactory = sessionFactory,
            sakService = sakService,
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            statistikkStønadRepo = statistikkStønadRepo,
            clock = clock,
            oppgaveKlient = oppgaveKlient,
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
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            statistikkStønadRepo = statistikkStønadRepo,
            navkontorService = navkontorService,
            sessionFactory = sessionFactory,
            clock = clock,
            simulerService = simulerService,
        )
    }

    open val meldekortApiHttpClient: MeldekortApiKlient by lazy {
        MeldekortApiHttpClient(
            baseUrl = Configuration.meldekortApiUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.meldekortApiScope) },
        )
    }
    val sendTilMeldekortApiService by lazy {
        SendTilMeldekortApiService(
            sakRepo = sakRepo,
            meldekortApiHttpClient = meldekortApiHttpClient,
        )
    }

    val oppgaveMeldekortService by lazy {
        OppgaveMeldekortService(
            oppgaveKlient = oppgaveKlient,
            sakRepo = sakRepo,
            brukersMeldekortRepo = brukersMeldekortRepo,
        )
    }

    val underkjennMeldekortBehandlingService by lazy {
        UnderkjennMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            tilgangsstyringService = tilgangsstyringService,
            clock = clock,
        )
    }

    val overtaMeldekortBehandlingService by lazy {
        OvertaMeldekortBehandlingService(
            tilgangsstyringService = tilgangsstyringService,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }

    val taMeldekortBehandlingService by lazy {
        TaMeldekortBehandlingService(
            tilgangsstyringService = tilgangsstyringService,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }

    val leggTilbakeMeldekortBehandlingService by lazy {
        LeggTilbakeMeldekortBehandlingService(
            tilgangsstyringService = tilgangsstyringService,
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
            tilgangsstyringService = tilgangsstyringService,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
        )
    }
}
