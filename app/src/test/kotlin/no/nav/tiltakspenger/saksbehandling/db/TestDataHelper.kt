package no.nav.tiltakspenger.saksbehandling.db

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.common.TestSaksnummerGenerator
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.benk.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.meldekort.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.person.PersonPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.sak.SakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.statistikk.sak.StatistikkSakRepoImpl
import no.nav.tiltakspenger.saksbehandling.repository.statistikk.stønad.StatistikkStønadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.søknad.PostgresSøknadRepo
import no.nav.tiltakspenger.saksbehandling.repository.utbetaling.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.vedtak.RammevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import javax.sql.DataSource

internal class TestDataHelper(
    private val dataSource: DataSource,
    val saksnummerGenerator: TestSaksnummerGenerator,
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val søknadRepo = PostgresSøknadRepo(sessionFactory)
    val behandlingRepo = BehandlingPostgresRepo(sessionFactory)
    val vedtakRepo = RammevedtakPostgresRepo(sessionFactory)
    val sakRepo = SakPostgresRepo(sessionFactory, saksnummerGenerator)
    val saksoversiktRepo = BenkOversiktPostgresRepo(sessionFactory)
    val statistikkSakRepo = StatistikkSakRepoImpl(sessionFactory)
    val statistikkStønadRepo = StatistikkStønadPostgresRepo(sessionFactory)
    val meldekortRepo = MeldekortBehandlingPostgresRepo(sessionFactory)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val meldekortBrukerRepo = BrukersMeldekortPostgresRepo(sessionFactory)
    val utbetalingsvedtakRepo: UtbetalingsvedtakRepo = UtbetalingsvedtakPostgresRepo(sessionFactory)
    val personRepo = PersonPostgresRepo(sessionFactory)
    val tiltaksdeltakerKafkaRepository = TiltaksdeltakerKafkaRepository(sessionFactory)
}

private val dbManager = TestDatabaseManager()

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(runIsolated: Boolean = false, test: (TestDataHelper) -> Unit) {
    dbManager.withMigratedDb(runIsolated, test)
}
