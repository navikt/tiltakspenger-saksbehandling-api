package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererInnvilgelsesvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.GenererStansvedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.JournalførVedtaksbrevGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingServiceImpl
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering.DistribuerVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring.JournalførRammevedtakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.person.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseGateway
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.repo.RammevedtakPostgresRepo
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
    gitHash: String,
    journalførVedtaksbrevGateway: JournalførVedtaksbrevGateway,
    genererVedtaksbrevGateway: GenererInnvilgelsesvedtaksbrevGateway,
    genererStansvedtaksbrevGateway: GenererStansvedtaksbrevGateway,
    tilgangsstyringService: TilgangsstyringService,
    personService: no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService,
    dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    navIdentClient: NavIdentClient,
    sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    tiltaksdeltagelseGateway: TiltaksdeltagelseGateway,
    oppgaveGateway: OppgaveGateway,
    clock: Clock,
) {
    open val rammevedtakRepo: RammevedtakRepo by lazy { RammevedtakPostgresRepo(sessionFactory as PostgresSessionFactory) }
    open val behandlingRepo: BehandlingRepo by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo(
            sessionFactory as PostgresSessionFactory,
        )
    }
    val behandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingServiceImpl(
            behandlingRepo = behandlingRepo,
            sessionFactory = sessionFactory,
            tilgangsstyringService = tilgangsstyringService,
            personService = personService,
            clock = clock,
        )
    }
    val startSøknadsbehandlingService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService(
            sakService = sakService,
            sessionFactory = sessionFactory,
            tilgangsstyringService = tilgangsstyringService,
            gitHash = gitHash,
            behandlingRepo = behandlingRepo,
            statistikkSakRepo = statistikkSakRepo,
            oppdaterSaksopplysningerService = oppdaterSaksopplysningerService,
            clock = clock,
        )
    }
    val oppdaterSaksopplysningerService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService(
            sakService = sakService,
            personService = personService,
            tiltaksdeltagelseGateway = tiltaksdeltagelseGateway,
            behandlingRepo = behandlingRepo,
        )
    }
    val oppdaterBegrunnelseVilkårsvurderingService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
        )
    }

    val oppdaterFritekstTilVedtaksbrevService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterFritekstTilVedtaksbrevService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
        )
    }
    val iverksettBehandlingService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService(
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
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            clock = clock,
        )
    }
    val startRevurderingService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.StartRevurderingService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.sak.StartRevurderingService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
            tilgangsstyringService = tilgangsstyringService,
            saksopplysningerService = oppdaterSaksopplysningerService,
            clock = clock,
        )
    }
    val oppdaterBarnetilleggService: no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService(
            sakService = sakService,
            behandlingRepo = behandlingRepo,
        )
    }

    val journalførVedtaksbrevService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring.JournalførRammevedtakService(
            journalførVedtaksbrevGateway = journalførVedtaksbrevGateway,
            rammevedtakRepo = rammevedtakRepo,
            genererInnvilgelsesvedtaksbrevGateway = genererVedtaksbrevGateway,
            personService = personService,
            navIdentClient = navIdentClient,
            genererStansvedtaksbrevGateway = genererStansvedtaksbrevGateway,
            clock = clock,
        )
    }

    val distribuerVedtaksbrevService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering.DistribuerVedtaksbrevService(
            dokumentdistribusjonsklient = dokumentdistribusjonsklient,
            rammevedtakRepo = rammevedtakRepo,
            clock = clock,
        )
    }

    val forhåndsvisVedtaksbrevService by lazy {
        no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService(
            sakService = sakService,
            genererInnvilgelsesbrevClient = genererVedtaksbrevGateway,
            genererStansbrevClient = genererStansvedtaksbrevGateway,
            personService = personService,
            navIdentClient = navIdentClient,
        )
    }
}
