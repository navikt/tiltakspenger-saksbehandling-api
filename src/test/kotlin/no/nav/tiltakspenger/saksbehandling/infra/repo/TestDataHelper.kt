package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.benk.infra.repo.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonPostgresRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import no.nav.tiltakspenger.saksbehandling.sak.SaksnummerGeneratorForTest
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerHendelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.MeldekortvedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
import javax.sql.DataSource

internal class TestDataHelper(
    private val dataSource: DataSource,
    idGenerators: IdGenerators,
    val clock: TikkendeKlokke = TikkendeKlokke(),
) {
    val saksnummerGenerator: SaksnummerGeneratorForTest = idGenerators.saksnummerGenerator
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val søknadRepo = SøknadPostgresRepo(sessionFactory)
    val behandlingRepo = RammebehandlingPostgresRepo(sessionFactory, clock)
    val vedtakRepo = RammevedtakPostgresRepo(sessionFactory)
    val sakRepo = SakPostgresRepo(sessionFactory, saksnummerGenerator, clock)
    val statistikkSakRepo = SaksstatistikkPostgresRepo(sessionFactory)
    val statistikkStønadRepo = StatistikkStønadPostgresRepo(sessionFactory, clock)
    val statistikkMeldekortRepo = StatistikkMeldekortPostgresRepo()
    val statistikkRepo = StatistikkPostgresRepo(sessionFactory, clock)
    private val personFakeKlient = PersonFakeKlient(clock)
    val statistikkService = StatistikkService(
        personKlient = personFakeKlient,
        gitHash = "fake-git-hash",
        clock = clock,
        statistikkRepo = statistikkRepo,
    )
    val meldekortRepo = MeldekortbehandlingPostgresRepo(sessionFactory)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val meldekortBrukerRepo = BrukersMeldekortPostgresRepo(sessionFactory)
    val meldekortvedtakRepo: MeldekortvedtakRepo = MeldekortvedtakPostgresRepo(sessionFactory)
    val personRepo = PersonPostgresRepo(sessionFactory)
    val tiltaksdeltakerHendelsePostgresRepo = TiltaksdeltakerHendelsePostgresRepo(sessionFactory, clock)
    val personhendelseRepository = PersonhendelseRepository(sessionFactory, clock)
    val identhendelseRepository = IdenthendelseRepository(sessionFactory, clock)
    val benkOversiktRepo = BenkOversiktPostgresRepo(sessionFactory)
    val utbetalingRepo = UtbetalingPostgresRepo(sessionFactory)
    val klagebehandlingRepo = KlagebehandlingPostgresRepo(sessionFactory)
    val tilbakekrevingBehandlingRepo = TilbakekrevingBehandlingPostgresRepo(sessionFactory)
    val tiltaksdeltakerRepo = TiltaksdeltakerPostgresRepo(sessionFactory)
}

private val dbManager: TestDatabaseManager by lazy { TestDatabaseManager() }

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(
    runIsolated: Boolean = false,
    clock: TikkendeKlokke = TikkendeKlokke(),
    test: (TestDataHelper) -> Unit,
) {
    dbManager.withMigratedDbTestDataHelper(runIsolated, clock, test)
}
