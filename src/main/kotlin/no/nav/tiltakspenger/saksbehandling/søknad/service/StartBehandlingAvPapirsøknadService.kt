package no.nav.tiltakspenger.saksbehandling.søknad.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.StartBehandlingAvPapirsøknadCommand
import java.time.Clock

class StartBehandlingAvPapirsøknadService(
    private val clock: Clock,
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val søknadRepo: SøknadRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val journalpostService: ValiderJournalpostService,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startBehandlingAvPapirsøknad(
        kommando: StartBehandlingAvPapirsøknadCommand,
        saksnummer: Saksnummer,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Pair<Sak, Søknadsbehandling> {
        val sak = sakService.hentForSaksnummer(saksnummer)
        val validering = journalpostService.hentOgValiderJournalpost(sak.fnr, kommando.journalpostId)

        if (!validering.journalpostFinnes) {
            throw IllegalArgumentException("Journalpost ${kommando.journalpostId} finnes ikke")
        }

        val papirsøknad = Søknad.opprett(
            sak = sak,
            journalpostId = kommando.journalpostId.toString(),
            opprettet = kommando.opprettet,
            tidsstempelHosOss = nå(clock),
            personopplysninger = kommando.personopplysninger,
            søknadstiltak = kommando.søknadstiltak,
            barnetillegg = kommando.barnetillegg,
            kvp = kommando.kvp,
            intro = kommando.intro,
            institusjon = kommando.institusjon,
            etterlønn = kommando.etterlønn,
            gjenlevendepensjon = kommando.gjenlevendepensjon,
            alderspensjon = kommando.alderspensjon,
            sykepenger = kommando.sykepenger,
            supplerendeStønadAlder = kommando.supplerendeStønadAlder,
            supplerendeStønadFlyktning = kommando.supplerendeStønadFlyktning,
            jobbsjansen = kommando.jobbsjansen,
            trygdOgPensjon = kommando.trygdOgPensjon,
            antallVedlegg = kommando.antallVedlegg,
            manueltSattSøknadsperiode = kommando.manueltSattSøknadsperiode,
            søknadstype = Søknadstype.PAPIR,
        )

        val søknadsbehandling = Søknadsbehandling.opprett(
            sak = sak,
            søknad = papirsøknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
        )

        val opprettetBehandlingStatistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = søknadsbehandling,
        )

        sessionFactory.withTransactionContext { tx ->
            // TODO Statistikk for søknad?
            søknadRepo.lagre(papirsøknad, tx)
            behandlingRepo.lagre(søknadsbehandling, tx)
            statistikkSakRepo.lagre(opprettetBehandlingStatistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sak.id,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }
        val oppdatertSak = sak.leggTilSøknadsbehandling(søknadsbehandling)
        MetricRegister.STARTET_BEHANDLING.inc()
        MetricRegister.STARTET_BEHANDLING_PAPIRSØKNAD.inc()
        return (oppdatertSak to søknadsbehandling)
    }
}
