package no.nav.tiltakspenger.saksbehandling.meldekort.infra.setup

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
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
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiHttpClientGateway
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppdaterMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OppgaveMeldekortService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldeperiodeTilBrukerService
import no.nav.tiltakspenger.saksbehandling.meldekort.service.UnderkjennMeldekortBehandlingService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
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
    oppgaveGateway: OppgaveGateway,
    sakRepo: SakRepo,
    clock: Clock,
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
            sessionFactory = sessionFactory,
            sakService = sakService,
            utbetalingsvedtakRepo = utbetalingsvedtakRepo,
            statistikkStønadRepo = statistikkStønadRepo,
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
            clock = clock,
        )
    }
    val oppdaterMeldekortService by lazy {
        OppdaterMeldekortService(
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            sakService = sakService,
            clock = clock,
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

    open val meldekortApiHttpClient: MeldekortApiHttpClientGateway by lazy {
        MeldekortApiHttpClient(
            baseUrl = Configuration.meldekortApiUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.meldekortApiScope) },
        )
    }
    val sendMeldeperiodeTilBrukerService by lazy {
        SendMeldeperiodeTilBrukerService(
            meldeperiodeRepo = meldeperiodeRepo,
            meldekortApiHttpClient = meldekortApiHttpClient,
            clock = clock,
        )
    }

    val oppgaveMeldekortService by lazy {
        OppgaveMeldekortService(
            oppgaveGateway = oppgaveGateway,
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
}
