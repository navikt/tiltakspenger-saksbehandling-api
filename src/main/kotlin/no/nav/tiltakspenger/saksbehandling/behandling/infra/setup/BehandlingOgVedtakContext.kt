package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettBehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering.DistribuerVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring.JournalførRammevedtakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import java.time.Clock

/**
 * TODO jah: Splitt ut vedtak og behandling til egne contexter.
 */
open class BehandlingOgVedtakContext(
    sessionFactory: SessionFactory,
    meldekortBehandlingRepo: MeldekortBehandlingRepo,
    meldeperiodeRepo: MeldeperiodeRepo,
    statistikkSakRepo: StatistikkSakRepo,
    statistikkStønadRepo: StatistikkStønadRepo,
    journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient,
    genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient,
    genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient,
    genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient,
    personService: PersonService,
    dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    navIdentClient: NavIdentClient,
    sakService: SakService,
    tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    statistikkSakService: StatistikkSakService,
    clock: Clock,
    sokosUtbetaldataClient: SokosUtbetaldataClient,
    navkontorService: NavkontorService,
    simulerService: SimulerService,
    personKlient: PersonKlient,
    oppgaveKlient: OppgaveKlient,
    tiltakspengerArenaClient: TiltakspengerArenaClient,
    tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    open val rammevedtakRepo: RammevedtakRepo by lazy { RammevedtakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val behandlingRepo: BehandlingRepo by lazy {
        BehandlingPostgresRepo(
            sessionFactory as PostgresSessionFactory,
        )
    }
    val behandlingService: BehandlingService by lazy {
        BehandlingService(
            behandlingRepo = behandlingRepo,
            sessionFactory = sessionFactory,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sakService = sakService,
        )
    }
    val startSøknadsbehandlingService: StartSøknadsbehandlingService by lazy {
        StartSøknadsbehandlingService(
            sakService = sakService,
            sessionFactory = sessionFactory,
            behandlingRepo = behandlingRepo,
            statistikkSakRepo = statistikkSakRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            clock = clock,
            statistikkSakService = statistikkSakService,
            personKlient = personKlient,
            oppgaveKlient = oppgaveKlient,
        )
    }
    val delautomatiskBehandlingService: DelautomatiskBehandlingService by lazy {
        DelautomatiskBehandlingService(
            behandlingRepo = behandlingRepo,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
            sakService = sakService,
            navkontorService = navkontorService,
            simulerService = simulerService,
            clock = clock,
        )
    }
    val behandleSøknadPåNyttService: BehandleSøknadPåNyttService by lazy {
        BehandleSøknadPåNyttService(
            sakService = sakService,
            sessionFactory = sessionFactory,
            statistikkSakRepo = statistikkSakRepo,
            statistikkSakService = statistikkSakService,
            clock = clock,
            behandlingRepo = behandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
        )
    }
    val hentSaksopplysingerService: HentSaksopplysingerService by lazy {
        HentSaksopplysingerService(
            hentPersonopplysninger = personService::hentPersonopplysninger,
            tiltaksdeltakelseKlient = tiltaksdeltakelseKlient,
            sokosUtbetaldataClient = sokosUtbetaldataClient,
            clock = clock,
            tiltakspengerArenaClient = tiltakspengerArenaClient,
            tiltaksdeltakerRepo = tiltaksdeltakerRepo,
        )
    }
    val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService by lazy {
        OppdaterSaksopplysningerService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
        )
    }
    val iverksettBehandlingService by lazy {
        IverksettBehandlingService(
            behandlingRepo = behandlingRepo,
            rammevedtakRepo = rammevedtakRepo,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            sessionFactory = sessionFactory,
            statistikkSakRepo = statistikkSakRepo,
            statistikkStønadRepo = statistikkStønadRepo,
            sakService = sakService,
            clock = clock,
            statistikkSakService = statistikkSakService,
        )
    }

    val sendBehandlingTilBeslutningService by lazy {
        SendBehandlingTilBeslutningService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
        )
    }
    val startRevurderingService: StartRevurderingService by lazy {
        StartRevurderingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
        )
    }

    val journalførVedtaksbrevService by lazy {
        JournalførRammevedtakService(
            journalførRammevedtaksbrevKlient = journalførRammevedtaksbrevKlient,
            rammevedtakRepo = rammevedtakRepo,
            genererVedtaksbrevForInnvilgelseKlient = genererVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererVedtaksbrevForAvslagKlient,
            personService = personService,
            navIdentClient = navIdentClient,
            genererVedtaksbrevForStansKlient = genererVedtaksbrevForStansKlient,
            sakService = sakService,
            clock = clock,
        )
    }

    val distribuerVedtaksbrevService by lazy {
        DistribuerVedtaksbrevService(
            dokumentdistribusjonsklient = dokumentdistribusjonsklient,
            rammevedtakRepo = rammevedtakRepo,
            clock = clock,
        )
    }

    val forhåndsvisVedtaksbrevService by lazy {
        ForhåndsvisVedtaksbrevService(
            sakService = sakService,
            genererInnvilgelsesbrevClient = genererVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererVedtaksbrevForAvslagKlient,
            genererStansbrevClient = genererVedtaksbrevForStansKlient,
            personService = personService,
            navIdentClient = navIdentClient,
        )
    }

    val taBehandlingService by lazy {
        TaBehandlingService(
            behandlingService = behandlingService,
            behandlingRepo = behandlingRepo,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    val overtaBehandlingService by lazy {
        OvertaBehandlingService(
            behandlingService = behandlingService,
            behandlingRepo = behandlingRepo,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
        )
    }

    val leggTilbakeBehandlingService by lazy {
        LeggTilbakeBehandlingService(
            behandlingService = behandlingService,
            behandlingRepo = behandlingRepo,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    val oppdaterBehandlingService by lazy {
        OppdaterBehandlingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            navkontorService = navkontorService,
            clock = clock,
            simulerService = simulerService,
            sessionFactory = sessionFactory,
        )
    }

    val settBehandlingPåVentService by lazy {
        SettBehandlingPåVentService(
            behandlingService = behandlingService,
            statistikkSakService = statistikkSakService,
            clock = clock,
        )
    }

    val gjenopptaBehandlingService by lazy {
        GjenopptaBehandlingService(
            behandlingService = behandlingService,
            hentSaksopplysingerService = hentSaksopplysingerService,
            statistikkSakService = statistikkSakService,
            clock = clock,
        )
    }

    val oppdaterSimuleringService by lazy {
        OppdaterSimuleringService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            meldekortBehandlingRepo = meldekortBehandlingRepo,
            simulerService = simulerService,
            sessionFactory = sessionFactory,
        )
    }
}
