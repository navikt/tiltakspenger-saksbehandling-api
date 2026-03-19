package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import java.time.Clock

class StartSøknadsbehandlingService(
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
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
        val (behandling, statistikkhendelser) = Søknadsbehandling.opprettAutomatiskBehandling(
            sak = sak,
            søknad = soknad,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
        )
        val statistikkDTO = statistikkService.generer(statistikkhendelser)
        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(behandling, tx)
            statistikkService.lagre(statistikkDTO, tx)
            sakService.markerSkalSendesTilMeldekortApi(
                sakId = soknad.sakId,
                sessionContext = tx,
            )
        }

        MetricRegister.STARTET_BEHANDLING.inc()
        return behandling
    }
}
