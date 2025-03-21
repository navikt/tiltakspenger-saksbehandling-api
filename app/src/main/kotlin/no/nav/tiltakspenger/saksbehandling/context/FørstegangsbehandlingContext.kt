package no.nav.tiltakspenger.saksbehandling.context

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.distribusjon.DokdistGateway
import no.nav.tiltakspenger.saksbehandling.felles.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.repository.vedtak.RammevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.JournalførVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.distribuering.DistribuerVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.journalføring.JournalførRammevedtakService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.StartRevurderingService
import java.time.Clock

open class FørstegangsbehandlingContext(
    sessionFactory: SessionFactory,
    meldekortBehandlingRepo: MeldekortBehandlingRepo,
    meldeperiodeRepo: MeldeperiodeRepo,
    statistikkSakRepo: StatistikkSakRepo,
    statistikkStønadRepo: StatistikkStønadRepo,
    gitHash: String,
    journalførVedtaksbrevGateway: JournalførVedtaksbrevGateway,
    genererVedtaksbrevGateway: GenererInnvilgelsesvedtaksbrevGateway,
    genererStansvedtaksbrevGateway: GenererStansvedtaksbrevGateway,
    tilgangsstyringService: TilgangsstyringService,
    personService: PersonService,
    dokdistGateway: DokdistGateway,
    navIdentClient: NavIdentClient,
    sakService: SakService,
    tiltakGateway: TiltakGateway,
    oppgaveGateway: OppgaveGateway,
    clock: Clock,
) {
    open val rammevedtakRepo: RammevedtakRepo by lazy { RammevedtakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val behandlingRepo: BehandlingRepo by lazy { BehandlingPostgresRepo(sessionFactory as PostgresSessionFactory) }
    val behandlingService: BehandlingService by lazy {
        BehandlingServiceImpl(
            behandlingRepo = behandlingRepo,
            sessionFactory = sessionFactory,
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
        )
    }
    val startSøknadsbehandlingService: StartSøknadsbehandlingService by lazy {
        StartSøknadsbehandlingService(
            sakService = sakService,
            sessionFactory = sessionFactory,
            tilgangsstyringService = tilgangsstyringService,
            gitHash = gitHash,
            behandlingRepo = behandlingRepo,
            statistikkSakRepo = statistikkSakRepo,
            oppdaterSaksopplysningerService = oppdaterSaksopplysningerService,
        )
    }
    val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService by lazy {
        OppdaterSaksopplysningerService(
            sakService = sakService,
            personService = personService,
            tiltakGateway = tiltakGateway,
            behandlingRepo = behandlingRepo,
        )
    }
    val oppdaterBegrunnelseVilkårsvurderingService by lazy {
        OppdaterBegrunnelseVilkårsvurderingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
        )
    }
    val oppdaterFritekstTilVedtaksbrevService by lazy {
        OppdaterFritekstTilVedtaksbrevService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
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
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
            gitHash = gitHash,
            sakService = sakService,
            oppgaveGateway = oppgaveGateway,
            clock = clock,
        )
    }

    val sendBehandlingTilBeslutningService by lazy {
        SendBehandlingTilBeslutningService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
        )
    }
    val startRevurderingService: StartRevurderingService by lazy {
        StartRevurderingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            tilgangsstyringService = tilgangsstyringService,
            saksopplysningerService = oppdaterSaksopplysningerService,
        )
    }
    val oppdaterBarnetilleggService: OppdaterBarnetilleggService by lazy {
        OppdaterBarnetilleggService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
        )
    }

    val journalførVedtaksbrevService by lazy {
        JournalførRammevedtakService(
            journalførVedtaksbrevGateway = journalførVedtaksbrevGateway,
            rammevedtakRepo = rammevedtakRepo,
            genererInnvilgelsesvedtaksbrevGateway = genererVedtaksbrevGateway,
            personService = personService,
            navIdentClient = navIdentClient,
            genererStansvedtaksbrevGateway = genererStansvedtaksbrevGateway,
        )
    }

    val distribuerVedtaksbrevService by lazy {
        DistribuerVedtaksbrevService(
            dokdistGateway = dokdistGateway,
            rammevedtakRepo = rammevedtakRepo,
        )
    }

    val forhåndsvisVedtaksbrevService by lazy {
        ForhåndsvisVedtaksbrevService(
            sakService = sakService,
            genererInnvilgelsesbrevClient = genererVedtaksbrevGateway,
            genererStansbrevClient = genererStansvedtaksbrevGateway,
            personService = personService,
            navIdentClient = navIdentClient,
        )
    }
}
