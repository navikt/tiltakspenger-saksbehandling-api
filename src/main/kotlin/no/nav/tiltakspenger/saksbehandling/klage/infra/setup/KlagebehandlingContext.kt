package no.nav.tiltakspenger.saksbehandling.klage.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandleSøknadPåNyttService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.GjenopptaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.LeggTilbakeRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.SettRammebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartRevurderingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.KabalHttpClient
import no.nav.tiltakspenger.saksbehandling.klage.infra.jobb.DistribuerKlagebrevJobb
import no.nav.tiltakspenger.saksbehandling.klage.infra.jobb.JournalførKlagebrevJobb
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehendelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.GenererKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.klage.ports.JournalførKlagebrevKlient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KabalClient
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagehendelseRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagevedtakRepo
import no.nav.tiltakspenger.saksbehandling.klage.service.AvbrytKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.ForhåndsvisBrevKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.GjenopptaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.IverksettAvvistKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.LeggTilbakeKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingFormkravService
import no.nav.tiltakspenger.saksbehandling.klage.service.OppdaterKlagebehandlingTekstTilBrevService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettRammebehandlingFraKlageService
import no.nav.tiltakspenger.saksbehandling.klage.service.OpprettholdKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.OvertaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.SettKlagebehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.klage.service.TaKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.klage.service.VurderKlagebehandlingService
import no.nav.tiltakspenger.saksbehandling.saksbehandler.NavIdentClient
import java.time.Clock

open class KlagebehandlingContext(
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
    private val clock: Clock,
    private val validerJournalpostService: ValiderJournalpostService,
    private val personService: PersonService,
    private val navIdentClient: NavIdentClient,
    private val genererKlagebrevKlient: GenererKlagebrevKlient,
    private val journalførKlagevedtaksbrevKlient: JournalførKlagebrevKlient,
    private val dokumentdistribusjonsklient: Dokumentdistribusjonsklient,
    private val behandleSøknadPåNyttService: BehandleSøknadPåNyttService,
    private val startRevurderingService: StartRevurderingService,
    private val taRammebehandlingService: TaRammebehandlingService,
    private val overtaRammebehandlingService: OvertaRammebehandlingService,
    private val leggTilbakeRammebehandlingService: LeggTilbakeRammebehandlingService,
    private val gjenopptaRammebehandlingService: GjenopptaRammebehandlingService,
    private val settRammebehandlingPåVentService: SettRammebehandlingPåVentService,
    private val texasClient: TexasClient,
) {

    open val klagebehandlingRepo: KlagebehandlingRepo by lazy {
        KlagebehandlingPostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val klagevedtakRepo: KlagevedtakRepo by lazy {
        KlagevedtakPostgresRepo(sessionFactory as PostgresSessionFactory)
    }
    open val klagehendelseRepo: KlagehendelseRepo by lazy {
        KlagehendelsePostgresRepo(sessionFactory as PostgresSessionFactory)
    }

    open val kabalClient: KabalClient by lazy {
        KabalHttpClient(
            baseUrl = Configuration.kabalUrl,
            getToken = { texasClient.getSystemToken(Configuration.kabalScope, IdentityProvider.AZUREAD) },
            clock = clock,
        )
    }

    open val opprettKlagebehandlingService: OpprettKlagebehandlingService by lazy {
        OpprettKlagebehandlingService(
            sakService = sakService,
            clock = clock,
            validerJournalpostService = validerJournalpostService,
            klagebehandlingRepo = klagebehandlingRepo,
        )
    }

    open val oppdaterKlagebehandlingFormkravService: OppdaterKlagebehandlingFormkravService by lazy {
        OppdaterKlagebehandlingFormkravService(
            sakService = sakService,
            clock = clock,
            validerJournalpostService = validerJournalpostService,
            klagebehandlingRepo = klagebehandlingRepo,
        )
    }
    open val avbrytKlagebehandlingService: AvbrytKlagebehandlingService by lazy {
        AvbrytKlagebehandlingService(
            sakService = sakService,
            clock = clock,
            klagebehandlingRepo = klagebehandlingRepo,
        )
    }

    open val forhåndsvisBrevKlagebehandlingService: ForhåndsvisBrevKlagebehandlingService by lazy {
        ForhåndsvisBrevKlagebehandlingService(
            sakService = sakService,
            clock = clock,
            personService = personService,
            navIdentClient = navIdentClient,
            genererKlagebrevKlient = genererKlagebrevKlient,
        )
    }
    open val oppdaterKlagebehandlingTekstTilBrevService: OppdaterKlagebehandlingTekstTilBrevService by lazy {
        OppdaterKlagebehandlingTekstTilBrevService(
            sakService = sakService,
            clock = clock,
            klagebehandlingRepo = klagebehandlingRepo,
        )
    }
    open val iverksettAvvistKlagebehandlingService: IverksettAvvistKlagebehandlingService by lazy {
        IverksettAvvistKlagebehandlingService(
            sakService = sakService,
            clock = clock,
            klagebehandlingRepo = klagebehandlingRepo,
            klagevedtakRepo = klagevedtakRepo,
            sessionFactory = sessionFactory,
        )
    }

    open val journalførKlagevedtakService: JournalførKlagebrevJobb by lazy {
        JournalførKlagebrevJobb(
            journalførKlagevedtaksbrevKlient = journalførKlagevedtaksbrevKlient,
            klagevedtakRepo = klagevedtakRepo,
            klagebehandlingRepo = klagebehandlingRepo,
            genererKlagebrevKlient = genererKlagebrevKlient,
            personService = personService,
            navIdentClient = navIdentClient,
            clock = clock,
        )
    }

    open val distribuerKlagevedtaksbrevService by lazy {
        DistribuerKlagebrevJobb(
            dokumentdistribusjonsklient = dokumentdistribusjonsklient,
            klagevedtakRepo = klagevedtakRepo,
            klagebehandlingRepo = klagebehandlingRepo,
            clock = clock,
        )
    }
    open val vurderKlagebehandlingService by lazy {
        VurderKlagebehandlingService(
            sakService = sakService,
            klagebehandlingRepo = klagebehandlingRepo,
            clock = clock,
        )
    }
    open val opprettRammebehandlingFraKlageService by lazy {
        OpprettRammebehandlingFraKlageService(
            sakService = sakService,
            behandleSøknadPåNyttService = behandleSøknadPåNyttService,
            startRevurderingService = startRevurderingService,
        )
    }
    open val overtaKlagebehandlingService by lazy {
        OvertaKlagebehandlingService(
            sakService = sakService,
            overtaRammebehandlingService = overtaRammebehandlingService,
            klagebehandlingRepo = klagebehandlingRepo,
            clock = clock,
        )
    }
    open val taKlagebehandlingService by lazy {
        TaKlagebehandlingService(
            sakService = sakService,
            klagebehandlingRepo = klagebehandlingRepo,
            taRammebehandlingService = taRammebehandlingService,
            clock = clock,
        )
    }
    open val leggTilbakeKlagebehandlingService by lazy {
        LeggTilbakeKlagebehandlingService(
            sakService = sakService,
            klagebehandlingRepo = klagebehandlingRepo,
            leggTilbakeRammebehandlingService = leggTilbakeRammebehandlingService,
            clock = clock,
        )
    }
    open val gjenopptaKlagebehandlingService by lazy {
        GjenopptaKlagebehandlingService(
            sakService = sakService,
            gjenopptaRammebehandlingService = gjenopptaRammebehandlingService,
            klagebehandlingRepo = klagebehandlingRepo,
            clock = clock,
        )
    }
    open val settKlagebehandlingPåVentService by lazy {
        SettKlagebehandlingPåVentService(
            sakService = sakService,
            settRammebehandlingPåVentService = settRammebehandlingPåVentService,
            klagebehandlingRepo = klagebehandlingRepo,
            clock = clock,
        )
    }
    open val opprettholdKlagebehandlingService by lazy {
        OpprettholdKlagebehandlingService(
            sakService = sakService,
            klagebehandlingRepo = klagebehandlingRepo,
        )
    }
}
