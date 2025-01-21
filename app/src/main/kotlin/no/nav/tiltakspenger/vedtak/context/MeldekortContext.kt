package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.service.IverksettMeldekortService
import no.nav.tiltakspenger.meldekort.service.OpprettMeldekortBehandlingService
import no.nav.tiltakspenger.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.meldekort.service.SendMeldeperiodeTilBrukerService
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.utbetaling.service.NavkontorService
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.clients.meldekort.MeldekortApiHttpClient
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldekortPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.meldekort.MeldeperiodePostgresRepo

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
) {
    open val meldekortRepo: MeldekortRepo by lazy {
        MeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy {
        MeldeperiodePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    val iverksettMeldekortService by lazy {
        IverksettMeldekortService(
            meldekortRepo = meldekortRepo,
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
        SendMeldekortTilBeslutterService(
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
            meldekortRepo = meldekortRepo,
            sakService = sakService,
        )
    }
    val opprettMeldekortBehandlingService by lazy {
        OpprettMeldekortBehandlingService(
            meldekortRepo = meldekortRepo,
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
}
