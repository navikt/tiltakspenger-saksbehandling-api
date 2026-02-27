package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import java.time.Clock

class StartSøknadsbehandlingService(
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val oppgaveKlient: OppgaveKlient,
) {
    val logger = KotlinLogging.logger {}

    suspend fun opprettAutomatiskSoknadsbehandling(
        soknad: InnvilgbarSøknad,
        correlationId: CorrelationId,
    ): Søknadsbehandling {
        val pdlPerson = sakService.hentEnkelPersonMedSkjermingForSakId(soknad.sakId, correlationId).getOrThrow()
        if (pdlPerson.strengtFortrolig || pdlPerson.strengtFortroligUtland || pdlPerson.fortrolig || pdlPerson.skjermet) {
            logger.info { "Person har adressebeskyttelse eller er skjermet, oppretter oppgave i Gosys" }
            oppgaveKlient.opprettOppgave(
                fnr = soknad.fnr,
                journalpostId = JournalpostId(soknad.journalpostId),
                oppgavebehov = Oppgavebehov.NY_SOKNAD,
            )
        }
        val sak = sakService.hentForSakId(soknad.sakId)
        val behandling = Søknadsbehandling.opprettAutomatiskBehandling(
            sak = sak,
            søknad = soknad,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
        )

        val statistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = behandling,
        )

        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = soknad.sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }

        MetricRegister.STARTET_BEHANDLING.inc()
        return behandling
    }
}
