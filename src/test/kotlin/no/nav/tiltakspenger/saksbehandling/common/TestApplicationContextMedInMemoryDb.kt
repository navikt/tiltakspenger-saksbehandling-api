package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BenkOversiktFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.BrukersMeldekortFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldeperiodeFakeRepo
import no.nav.tiltakspenger.saksbehandling.person.infra.repo.PersonFakeRepo
import no.nav.tiltakspenger.saksbehandling.sak.IdGenerators
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakFakeRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkFakeRepo
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadFakeRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingFakeRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseFakeRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.MeldekortvedtakFakeRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.UtbetalingFakeRepo
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakFakeRepo

/**
 * In-memory-versjon av [TestApplicationContext].
 * Arver alle context-konfigurasjoner og fake-klienter fra basisklassen, og overstyrer `*RepoOverride`-hookene med fake-repoer.
 *
 * Inneholder ingen DB/Postgres-referanser.
 *
 * Klassen er `open` så enkelttester kan lage anonyme subklasser for å overstyre fake-klienter eller fake-repoer der det trengs spesialoppførsel (f.eks. spy, custom returverdier).
 */
open class TestApplicationContextMedInMemoryDb(
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
    clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    override val texasClient: TexasClient = TexasClientFake(clock),
    tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    idGenerators: IdGenerators = IdGenerators(),
) : TestApplicationContext(clock, idGenerators, tilgangsmaskinFakeClient) {
    // Fake-repoer (intern lagring)
    private val utbetalingFakeRepo = UtbetalingFakeRepo()
    private val rammevedtakFakeRepo = RammevedtakFakeRepo(utbetalingFakeRepo)
    private val meldekortvedtakFakeRepo = MeldekortvedtakFakeRepo(utbetalingFakeRepo)
    private val klagevedtakFakeRepo = KlagevedtakFakeRepo()
    private val statistikkFakeRepo = StatistikkFakeRepo()
    private val meldekortbehandlingFakeRepo = MeldekortbehandlingFakeRepo()
    private val meldeperiodeFakeRepo = MeldeperiodeFakeRepo()
    private val brukersMeldekortFakeRepo = BrukersMeldekortFakeRepo(meldeperiodeFakeRepo)
    private val rammebehandlingFakeRepo = RammebehandlingFakeRepo()
    private val klagebehandlingFakeRepo = KlagebehandlingFakeRepo()
    private val søknadFakeRepo = SøknadFakeRepo(rammebehandlingFakeRepo)
    private val tiltaksdeltakerFakeRepo = TiltaksdeltakerFakeRepo()
    private val tilbakekrevingBehandlingFakeRepo = TilbakekrevingBehandlingFakeRepo()
    private val tilbakekrevingHendelseFakeRepo = TilbakekrevingHendelseFakeRepo(clock)
    private val benkOversiktFakeRepo =
        BenkOversiktFakeRepo(søknadFakeRepo, rammebehandlingFakeRepo, meldekortbehandlingFakeRepo, klagebehandlingFakeRepo)
    private val sakFakeRepo =
        SakFakeRepo(
            behandlingRepo = rammebehandlingFakeRepo,
            rammevedtakRepo = rammevedtakFakeRepo,
            meldekortbehandlingRepo = meldekortbehandlingFakeRepo,
            meldeperiodeRepo = meldeperiodeFakeRepo,
            meldekortvedtakRepo = meldekortvedtakFakeRepo,
            klagevedtakRepo = klagevedtakFakeRepo,
            søknadFakeRepo = søknadFakeRepo,
            klagebehandlingFakeRepo = klagebehandlingFakeRepo,
            brukersMeldekortFakeRepo = brukersMeldekortFakeRepo,
            tilbakekrevingBehandlingFakeRepo = tilbakekrevingBehandlingFakeRepo,
            clock = clock,
        )
    private val personFakeRepo =
        PersonFakeRepo(sakFakeRepo, søknadFakeRepo, meldekortbehandlingFakeRepo, rammebehandlingFakeRepo)

    // Hekt fake-repoer på override-hookene fra basisklassen
    override val personRepoOverride = personFakeRepo
    override val sakRepoOverride = sakFakeRepo
    override val benkOversiktRepoOverride = benkOversiktFakeRepo
    override val tiltaksdeltakerRepoOverride = tiltaksdeltakerFakeRepo
    override val statistikkRepoOverride = statistikkFakeRepo
    override val søknadRepoOverride = søknadFakeRepo
    override val meldekortbehandlingRepoOverride = meldekortbehandlingFakeRepo
    override val meldeperiodeRepoOverride = meldeperiodeFakeRepo
    override val brukersMeldekortRepoOverride = brukersMeldekortFakeRepo
    override val rammevedtakRepoOverride = rammevedtakFakeRepo
    override val rammebehandlingRepoOverride = rammebehandlingFakeRepo
    override val klagebehandlingRepoOverride = klagebehandlingFakeRepo
    override val klagevedtakRepoOverride = klagevedtakFakeRepo
    override val meldekortvedtakRepoOverride = meldekortvedtakFakeRepo
    override val utbetalingRepoOverride = utbetalingFakeRepo
    override val tilbakekrevingHendelseRepoOverride = tilbakekrevingHendelseFakeRepo
    override val tilbakekrevingBehandlingRepoOverride = tilbakekrevingBehandlingFakeRepo
}
