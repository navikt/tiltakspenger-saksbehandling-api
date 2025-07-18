package no.nav.tiltakspenger.saksbehandling.infra.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.saksbehandling.benk.infra.repo.BenkOversiktPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonPostgresRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.sak.TestSaksnummerGenerator
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.vedtak.StatistikkStønadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingsvedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
import javax.sql.DataSource

internal class TestDataHelper(
    private val dataSource: DataSource,
    val saksnummerGenerator: TestSaksnummerGenerator,
    val clock: TikkendeKlokke = TikkendeKlokke(),
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val søknadRepo = SøknadPostgresRepo(sessionFactory)
    val behandlingRepo =
        no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo(sessionFactory)
    val vedtakRepo = RammevedtakPostgresRepo(sessionFactory)
    val sakRepo = SakPostgresRepo(sessionFactory, saksnummerGenerator, clock)
    val statistikkSakRepo = StatistikkSakPostgresRepo(sessionFactory)
    val statistikkStønadRepo = StatistikkStønadPostgresRepo(sessionFactory, clock)
    val meldekortRepo = MeldekortBehandlingPostgresRepo(sessionFactory)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val meldekortBrukerRepo = BrukersMeldekortPostgresRepo(sessionFactory)
    val utbetalingsvedtakRepo: UtbetalingsvedtakRepo = UtbetalingsvedtakPostgresRepo(sessionFactory)
    val personRepo = PersonPostgresRepo(sessionFactory)
    val tiltaksdeltakerKafkaRepository = TiltaksdeltakerKafkaRepository(sessionFactory)
    val personhendelseRepository = PersonhendelseRepository(sessionFactory)
    val identhendelseRepository = IdenthendelseRepository(sessionFactory)
    val benkOversiktRepo = BenkOversiktPostgresRepo(sessionFactory)
}

private val dbManager = TestDatabaseManager()

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(runIsolated: Boolean = false, test: (TestDataHelper) -> Unit) {
    dbManager.withMigratedDb(runIsolated, test)
}
