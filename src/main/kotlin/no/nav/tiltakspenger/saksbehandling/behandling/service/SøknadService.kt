package no.nav.tiltakspenger.saksbehandling.behandling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevLageHendelserRollen
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

class SøknadService(
    private val søknadRepo: SøknadRepo,
    private val oppgaveGateway: OppgaveGateway,
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
) {
    private val log = KotlinLogging.logger {}

    /** Skal i førsteomgang kun brukes til digitale søknader. Dersom en saksbehandler skal registere en papirsøknad må vi ha en egen funksjon som sjekker tilgang.*/
    suspend fun nySøknad(søknad: Søknad, systembruker: Systembruker) {
        krevLageHendelserRollen(systembruker)
        val oppgaveId =
            oppgaveGateway.opprettOppgave(søknad.fnr, JournalpostId(søknad.journalpostId), Oppgavebehov.NY_SOKNAD)
        log.info { "Opprettet oppgave med id $oppgaveId for søknad med id ${søknad.id}" }
        sessionFactory.withTransactionContext { tx ->
            søknadRepo.lagre(søknad.copy(oppgaveId = oppgaveId), tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = søknad.sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }
    }

    fun lagreAvbruttSøknad(søknad: Søknad, tx: TransactionContext) {
        søknadRepo.lagreAvbruttSøknad(søknad, tx)
    }

    fun harSoknadUnderBehandling(fnr: Fnr): Boolean {
        val apneSoknader = søknadRepo.hentApneSoknader(fnr)
        return apneSoknader.isNotEmpty()
    }
}
