package no.nav.tiltakspenger.saksbehandling.søknad.infra.setup

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.infra.jobb.DelautomatiskSøknadsbehandlingJobb
import no.nav.tiltakspenger.saksbehandling.behandling.service.SøknadService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.StartSøknadsbehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.DelautomatiskBehandlingService
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.MdcKorrelasjon
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.SøknadApiKlient
import no.nav.tiltakspenger.saksbehandling.søknad.infra.http.SøknadApiHttpClient
import no.nav.tiltakspenger.saksbehandling.søknad.infra.jobb.SendSøknadsutfallTilSøknadApiJobb
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadPostgresRepo
import no.nav.tiltakspenger.saksbehandling.søknad.service.StartBehandlingAvManueltRegistrertSøknadService
import java.time.Clock

open class SøknadContext(
    sessionFactory: SessionFactory,
    rammebehandlingRepo: RammebehandlingRepo,
    hentSaksopplysingerService: HentSaksopplysingerService,
    sakService: SakService,
    personService: PersonService,
    statistikkService: StatistikkService,
    clock: Clock,
    safJournalpostClient: SafJournalpostClient,
    personKlient: PersonKlient,
    texasClient: TexasClient,
    rammevedtakRepo: RammevedtakRepo,
    startSøknadsbehandlingService: StartSøknadsbehandlingService,
    delautomatiskBehandlingService: DelautomatiskBehandlingService,
    oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
) {
    open val søknadRepo: SøknadRepo by lazy { SøknadPostgresRepo(sessionFactory = sessionFactory as PostgresSessionFactory) }
    val søknadService: SøknadService by lazy {
        SøknadService(
            søknadRepo,
            sessionFactory,
            sakService,
        )
    }
    val startBehandlingAvManueltRegistrertSøknadService: StartBehandlingAvManueltRegistrertSøknadService by lazy {
        StartBehandlingAvManueltRegistrertSøknadService(
            sessionFactory = sessionFactory,
            rammebehandlingRepo = rammebehandlingRepo,
            hentSaksopplysingerService = hentSaksopplysingerService,
            personService = personService,
            sakService = sakService,
            statistikkService = statistikkService,
            søknadRepo = søknadRepo,
            journalpostService = validerJournalpostService,
            clock = clock,
        )
    }

    val validerJournalpostService: ValiderJournalpostService by lazy {
        ValiderJournalpostService(
            safJournalpostClient = safJournalpostClient,
            personKlient = personKlient,
        )
    }

    open val søknadApiKlient: SøknadApiKlient by lazy {
        SøknadApiHttpClient(
            baseUrl = Configuration.soknadApiUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.soknadApiScope,
            ),
        )
    }

    val sendSøknadsutfallTilSøknadApiJobb by lazy {
        SendSøknadsutfallTilSøknadApiJobb(
            rammevedtakRepo = rammevedtakRepo,
            søknadRepo = søknadRepo,
            søknadApiKlient = søknadApiKlient,
            clock = clock,
        )
    }

    val delautomatiskSøknadsbehandlingJobb by lazy {
        DelautomatiskSøknadsbehandlingJobb(
            søknadRepo = søknadRepo,
            rammebehandlingRepo = rammebehandlingRepo,
            startSøknadsbehandlingService = startSøknadsbehandlingService,
            delautomatiskBehandlingService = delautomatiskBehandlingService,
            oppdaterSaksopplysningerService = oppdaterSaksopplysningerService,
            clock = clock,
            korrelasjon = MdcKorrelasjon(),
        )
    }
}
