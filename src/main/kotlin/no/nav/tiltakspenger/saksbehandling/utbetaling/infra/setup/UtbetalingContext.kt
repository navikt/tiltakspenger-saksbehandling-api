package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.setup

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.GenererVedtaksbrevForMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.JournalførMeldekortKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingHttpKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt.UtbetalingsoversiktHttpKlient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt.UtbetalingsoversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.JournalførMeldekortvedtakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.OppdaterUtbetalingsoversiktJobb
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.OppdaterUtbetalingsstatusService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SendUtbetalingerService
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock
import java.time.LocalDateTime

open class UtbetalingContext(
    sessionFactory: SessionFactory,
    genererVedtaksbrevForMeldekortKlient: GenererVedtaksbrevForMeldekortKlient,
    journalførMeldekortKlient: JournalførMeldekortKlient,
    navIdentClient: NavIdentClient,
    personService: PersonService,
    texasClient: TexasClient,
    sakRepo: SakRepo,
    clock: Clock,
    navkontorService: NavkontorService,
    statistikkService: StatistikkService,
    meterRegistry: MeterRegistry,
    erDev: Boolean,
) {
    open val utbetalingsklient: Utbetalingsklient by lazy {
        UtbetalingHttpKlient(
            baseUrl = Configuration.utbetalingUrl,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.utbetalingScope,
            ),
            clock = clock,
        )
    }
    open val utbetalingsoversiktklient: Utbetalingsoversiktklient by lazy {
        UtbetalingsoversiktHttpKlient(
            baseUrl = Configuration.sokosUtbetaldataUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.sokosUtbetaldataScope,
            ),
            meterRegistry = meterRegistry,
        )
    }
    open val meldekortvedtakRepo: MeldekortvedtakRepo by lazy {
        MeldekortvedtakPostgresRepo(
            sessionFactory as PostgresSessionFactory,
        )
    }
    open val utbetalingRepo: UtbetalingRepo by lazy {
        UtbetalingPostgresRepo(
            sessionFactory as PostgresSessionFactory,
        )
    }
    open val utbetalingsoversiktRepo: UtbetalingsoversiktRepo by lazy {
        UtbetalingsoversiktPostgresRepo(
            sessionFactory as PostgresSessionFactory,
            // De to testsakene i dev ble opprettet 25.09.2026; starttidspunktet i prod avtales før jobben slås på der.
            oppfølgesFra = if (erDev) LocalDateTime.of(2026, 9, 25, 0, 0) else null,
        )
    }
    open val simulerService: SimulerService by lazy {
        SimulerService(
            utbetalingRepo = utbetalingRepo,
            navkontorService = navkontorService,
            utbetalingsklient = utbetalingsklient,
        )
    }
    val sendUtbetalingerService: SendUtbetalingerService by lazy {
        SendUtbetalingerService(
            utbetalingRepo = utbetalingRepo,
            meldekortvedtakRepo = meldekortvedtakRepo,
            utbetalingsklient = utbetalingsklient,
            statistikkService = statistikkService,
            clock = clock,
        )
    }
    val journalførMeldekortvedtakService by lazy {
        JournalførMeldekortvedtakService(
            meldekortvedtakRepo = meldekortvedtakRepo,
            journalførMeldekortKlient = journalførMeldekortKlient,
            genererVedtaksbrevForMeldekortKlient = genererVedtaksbrevForMeldekortKlient,
            navIdentClient = navIdentClient,
            personService = personService,
            sakRepo = sakRepo,
            clock = clock,
        )
    }

    val oppdaterUtbetalingsstatusService by lazy {
        OppdaterUtbetalingsstatusService(
            utbetalingRepo = utbetalingRepo,
            utbetalingsklient = utbetalingsklient,
            clock = clock,
        )
    }

    val oppdaterUtbetalingsoversiktJobb by lazy {
        OppdaterUtbetalingsoversiktJobb(
            sakRepo = sakRepo,
            utbetalingsoversiktRepo = utbetalingsoversiktRepo,
            utbetalingsoversiktklient = utbetalingsoversiktklient,
            clock = clock,
            meterRegistry = meterRegistry,
            // Lavt i dev til vi har målt hva et oppslag koster; justeres før jobben slås på i prod.
            limit = if (erDev) 2 else 10,
            logger = KotlinLogging.logger(OppdaterUtbetalingsoversiktJobb::class.java.name),
            sikkerlogg = Sikkerlogg,
        )
    }
}
