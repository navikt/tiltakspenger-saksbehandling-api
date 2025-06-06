package no.nav.tiltakspenger.saksbehandling.behandling.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
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
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBarnetilleggService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterBegrunnelseVilkårsvurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterFritekstTilVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SendBehandlingTilBeslutningService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.distribuering.DistribuerVedtaksbrevService
import no.nav.tiltakspenger.saksbehandling.behandling.service.journalføring.JournalførRammevedtakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient
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
    journalførRammevedtaksbrevKlient: JournalførRammevedtaksbrevKlient,
    genererVedtaksbrevForInnvilgelseKlient: GenererVedtaksbrevForInnvilgelseKlient,
    genererVedtaksbrevForAvslagKlient: GenererVedtaksbrevForAvslagKlient,
    genererVedtaksbrevForStansKlient: GenererVedtaksbrevForStansKlient,
    tilgangsstyringService: TilgangsstyringService,
    personService: PersonService,
    dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    navIdentClient: NavIdentClient,
    sakService: SakService,
    tiltaksdeltagelseKlient: TiltaksdeltagelseKlient,
    oppgaveKlient: OppgaveKlient,
    statistikkSakService: StatistikkSakService,
    clock: Clock,
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
            tilgangsstyringService = tilgangsstyringService,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
        )
    }
    val startSøknadsbehandlingService: StartSøknadsbehandlingService by lazy {
        StartSøknadsbehandlingService(
            sakService = sakService,
            sessionFactory = sessionFactory,
            behandlingRepo = behandlingRepo,
            statistikkSakRepo = statistikkSakRepo,
            oppdaterSaksopplysningerService = oppdaterSaksopplysningerService,
            clock = clock,
            statistikkSakService = statistikkSakService,
        )
    }
    val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService by lazy {
        OppdaterSaksopplysningerService(
            sakService = sakService,
            personService = personService,
            tiltaksdeltagelseKlient = tiltaksdeltagelseKlient,
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
            sakService = sakService,
            oppgaveKlient = oppgaveKlient,
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
            saksopplysningerService = oppdaterSaksopplysningerService,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
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
            journalførRammevedtaksbrevKlient = journalførRammevedtaksbrevKlient,
            rammevedtakRepo = rammevedtakRepo,
            genererVedtaksbrevForInnvilgelseKlient = genererVedtaksbrevForInnvilgelseKlient,
            genererVedtaksbrevForAvslagKlient = genererVedtaksbrevForAvslagKlient,
            personService = personService,
            navIdentClient = navIdentClient,
            genererVedtaksbrevForStansKlient = genererVedtaksbrevForStansKlient,
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
            tilgangsstyringService = tilgangsstyringService,
            behandlingRepo = behandlingRepo,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
        )
    }

    val overtaBehandlingService by lazy {
        OvertaBehandlingService(
            tilgangsstyringService = tilgangsstyringService,
            behandlingRepo = behandlingRepo,
            clock = clock,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
        )
    }

    val leggTilbakeBehandlingService by lazy {
        LeggTilbakeBehandlingService(
            tilgangsstyringService = tilgangsstyringService,
            behandlingRepo = behandlingRepo,
            statistikkSakService = statistikkSakService,
            statistikkSakRepo = statistikkSakRepo,
            sessionFactory = sessionFactory,
        )
    }
}
