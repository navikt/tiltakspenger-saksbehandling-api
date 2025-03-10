package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.clients.meldekort.MeldekortApiHttpClient
import no.nav.tiltakspenger.vedtak.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.vedtak.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.vedtak.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.vedtak.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.vedtak.meldekort.service.OppgaveMeldekortService
import no.nav.tiltakspenger.vedtak.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.vedtak.meldekort.service.SendMeldekortTilBeslutningService
import no.nav.tiltakspenger.vedtak.meldekort.service.SendMeldeperiodeTilBrukerService
import no.nav.tiltakspenger.vedtak.repository.meldekort.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SakRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.vedtak.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.vedtak.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.vedtak.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.vedtak.utbetaling.service.NavkontorService

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
        )
    }
    val sendMeldekortTilBeslutterService by lazy {
        SendMeldekortTilBeslutningService(
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            sakService = sakService,
        )
    }
    val opprettMeldekortBehandlingService by lazy {
        OpprettMeldekortBehandlingService(
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            brukersMeldekortRepo = brukersMeldekortRepo,
            sakService = sakService,
            navkontorService = navkontorService,
            sessionFactory = sessionFactory,
        )
    }

    private val meldekortApiHttpClient by lazy {
        MeldekortApiHttpClient(
            baseUrl = Configuration.meldekortApiUrl,
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.meldekortApiScope) },
        )
    }
    val sendMeldeperiodeTilBrukerService by lazy {
        SendMeldeperiodeTilBrukerService(
            meldeperiodeRepo = meldeperiodeRepo,
            meldekortApiHttpClient = meldekortApiHttpClient,
        )
    }

    val oppgaveMeldekortService by lazy {
        OppgaveMeldekortService(
            oppgaveGateway = oppgaveGateway,
            sakRepo = sakRepo,
            brukersMeldekortRepo = brukersMeldekortRepo,
        )
    }
}
