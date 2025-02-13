package no.nav.tiltakspenger.vedtak.context

import no.nav.tiltakspenger.distribusjon.ports.DokdistGateway
import no.nav.tiltakspenger.felles.NavIdentClient
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.ports.JournalførVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.behandling.StartSøknadsbehandlingV2Service
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.kvp.KvpVilkårService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.kvp.KvpVilkårServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårService
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.livsopphold.LivsoppholdVilkårServiceImpl
import no.nav.tiltakspenger.saksbehandling.service.behandling.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkårService
import no.nav.tiltakspenger.saksbehandling.service.distribuering.DistribuerVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.service.journalføring.JournalførRammevedtakService
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingPostgresRepo
import no.nav.tiltakspenger.vedtak.repository.vedtak.RammevedtakPostgresRepo

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
) {
    open val rammevedtakRepo: RammevedtakRepo by lazy { RammevedtakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val behandlingRepo: BehandlingRepo by lazy { BehandlingPostgresRepo(sessionFactory as PostgresSessionFactory) }
    val behandlingService: BehandlingService by lazy {
        BehandlingServiceImpl(
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
            tiltakGateway = tiltakGateway,
            oppgaveGateway = oppgaveGateway,
        )
    }
    val startSøknadsbehandlingV2Service: StartSøknadsbehandlingV2Service by lazy {
        StartSøknadsbehandlingV2Service(
            sakService = sakService,
            sessionFactory = sessionFactory,
            personService = personService,
            tilgangsstyringService = tilgangsstyringService,
            tiltakGateway = tiltakGateway,
            gitHash = gitHash,
            behandlingRepo = behandlingRepo,
            statistikkSakRepo = statistikkSakRepo,
        )
    }
    val tiltaksdeltagelseVilkårService: TiltaksdeltagelseVilkårService by lazy {
        TiltaksdeltagelseVilkårService(
            behandlingRepo = behandlingRepo,
            behandlingService = behandlingService,
        )
    }
    val kvpVilkårService: KvpVilkårService by lazy {
        KvpVilkårServiceImpl(
            behandlingService = behandlingService,
            behandlingRepo = behandlingRepo,
        )
    }
    val livsoppholdVilkårService: LivsoppholdVilkårService by lazy {
        LivsoppholdVilkårServiceImpl(
            behandlingService = behandlingService,
            behandlingRepo = behandlingRepo,
        )
    }
    val startRevurderingService: StartRevurderingService by lazy {
        StartRevurderingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            tilgangsstyringService = tilgangsstyringService,
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
}
