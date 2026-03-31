package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.RammebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForAvslagKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForInnvilgelseKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForOpphørKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererVedtaksbrevForStansKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførRammevedtaksbrevKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterBeregningOgSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendRammebehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisRammevedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering.DistribuerRammevedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring.JournalførRammevedtakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient
import java.time.Clock

open class BehandlingOgVedtakContext(
    sessionFactory: SessionFactory,
    meldekortbehandlingRepo: MeldekortbehandlingRepo,
    meldeperiodeRepo: MeldeperiodeRepo,
    statistikkService: StatistikkService,
    journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient,
    genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient,
    genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient,
    genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient,
    genererVedtaksbrevForOpphørKlient: GenererVedtaksbrevForOpphørKlient,
    personService: PersonService,
    dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    navIdentClient: NavIdentClient,
    sakService: SakService,
    tiltaksdeltakelseKlient: TiltaksdeltakelseKlient,
    clock: Clock,
    sokosUtbetaldataClient: SokosUtbetaldataClient,
    navkontorService: NavkontorService,
    simulerService: SimulerService,
    oppgaveKlient: OppgaveKlient,
    tiltakspengerArenaClient: TiltakspengerArenaClient,
    tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    open val rammevedtakRepo: RammevedtakRepo by lazy { RammevedtakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val rammebehandlingRepo: RammebehandlingRepo by lazy {
        RammebehandlingPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }
    val rammebehandlingService: RammebehandlingService by lazy {
        RammebehandlingService(
            rammebehandlingRepo = rammebehandlingRepo,
            sessionFactory = sessionFactory,
            clock = clock,
            sakService = sakService,
            statistikkService = statistikkService,
        )
    }
    val startSøknadsbehandlingService: StartSøknadsbehandlingService by lazy {
        StartSøknadsbehandlingService(
            sakService = sakService,
            sessionFactory = sessionFactory,
            rammebehandlingRepo = rammebehandlingRepo,
            statistikkService = statistikkService,
            hentSaksopplysingerService = hentSaksopplysingerService,
            clock = clock,
            oppgaveKlient = oppgaveKlient,
        )
    }
    val delautomatiskBehandlingService: DelautomatiskBehandlingService by lazy {
        DelautomatiskBehandlingService(
            rammebehandlingRepo = rammebehandlingRepo,
            statistikkService = statistikkService,
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
            clock = clock,
            rammebehandlingRepo = rammebehandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            statistikkService = statistikkService,
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
            rammebehandlingRepo = rammebehandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
        )
    }
    val iverksettRammebehandlingService by lazy {
        IverksettRammebehandlingService(
            rammebehandlingRepo = rammebehandlingRepo,
            rammevedtakRepo = rammevedtakRepo,
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            sessionFactory = sessionFactory,
            sakService = sakService,
            clock = clock,
            statistikkService = statistikkService,
            oppdaterBeregningOgSimuleringService = oppdaterBeregningOgSimuleringService,
        )
    }

    val sendRammebehandlingTilBeslutningService by lazy {
        SendRammebehandlingTilBeslutningService(
            sakService = sakService,
            rammebehandlingRepo = rammebehandlingRepo,
            oppdaterBeregningOgSimuleringService = oppdaterBeregningOgSimuleringService,
            clock = clock,
            sessionFactory = sessionFactory,
            statistikkService = statistikkService,
        )
    }
    val startRevurderingService: StartRevurderingService by lazy {
        StartRevurderingService(
            sakService = sakService,
            rammebehandlingRepo = rammebehandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            clock = clock,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
        )
    }

    val journalførRammevedtaksbrevService by lazy {
        JournalførRammevedtakService(
            journalførRammevedtaksbrevKlient = journalførRammevedtaksbrevKlient,
            rammevedtakRepo = rammevedtakRepo,
            genererVedtaksbrevForInnvilgelseKlient = genererVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererVedtaksbrevForAvslagKlient,
            genererVedtaksbrevForStansKlient = genererVedtaksbrevForStansKlient,
            genererVedtaksbrevForOpphørKlient = genererVedtaksbrevForOpphørKlient,
            personService = personService,
            navIdentClient = navIdentClient,
            clock = clock,
        )
    }

    val distribuerRammevedtaksbrevService by lazy {
        DistribuerRammevedtaksbrevService(
            dokumentdistribusjonsklient = dokumentdistribusjonsklient,
            rammevedtakRepo = rammevedtakRepo,
            clock = clock,
        )
    }

    val forhåndsvisRammevedtaksbrevService by lazy {
        ForhåndsvisRammevedtaksbrevService(
            sakService = sakService,
            genererInnvilgelsesbrevClient = genererVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererVedtaksbrevForAvslagKlient,
            genererStansbrevClient = genererVedtaksbrevForStansKlient,
            genererOpphørbrevKlient = genererVedtaksbrevForOpphørKlient,
            personService = personService,
            navIdentClient = navIdentClient,
            clock = clock,
        )
    }

    val taRammebehandlingService by lazy {
        TaRammebehandlingService(
            behandlingService = rammebehandlingService,
            rammebehandlingRepo = rammebehandlingRepo,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    val overtaRammebehandlingService by lazy {
        OvertaRammebehandlingService(
            rammebehandlingService = rammebehandlingService,
            rammebehandlingRepo = rammebehandlingRepo,
            clock = clock,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
        )
    }

    val leggTilbakeRammebehandlingService by lazy {
        LeggTilbakeRammebehandlingService(
            behandlingService = rammebehandlingService,
            rammebehandlingRepo = rammebehandlingRepo,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    val oppdaterRammebehandlingService by lazy {
        OppdaterRammebehandlingService(
            sakService = sakService,
            rammebehandlingRepo = rammebehandlingRepo,
            navkontorService = navkontorService,
            clock = clock,
            simulerService = simulerService,
            sessionFactory = sessionFactory,
        )
    }

    val settRammebehandlingPåVentService by lazy {
        SettRammebehandlingPåVentService(
            behandlingService = rammebehandlingService,
            clock = clock,
        )
    }

    val gjenopptaRammebehandlingService by lazy {
        GjenopptaRammebehandlingService(
            behandlingService = rammebehandlingService,
            hentSaksopplysingerService = hentSaksopplysingerService,
            clock = clock,
        )
    }

    val oppdaterBeregningOgSimuleringService by lazy {
        OppdaterBeregningOgSimuleringService(
            sakService = sakService,
            rammebehandlingRepo = rammebehandlingRepo,
            meldekortbehandlingRepo = meldekortbehandlingRepo,
            simulerService = simulerService,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }
}
