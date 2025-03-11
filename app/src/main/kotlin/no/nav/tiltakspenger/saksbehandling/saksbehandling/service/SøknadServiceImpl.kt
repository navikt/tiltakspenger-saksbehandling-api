package no.nav.tiltakspenger.saksbehandling.saksbehandling.service

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.Oppgavebehov
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SøknadRepo

class SøknadServiceImpl(
    private val søknadRepo: SøknadRepo,
    private val oppgaveGateway: OppgaveGateway,
) : SøknadService {
    private val log = KotlinLogging.logger {}

    override suspend fun nySøknad(søknad: Søknad, systembruker: Systembruker) {
        require(systembruker.roller.harLageHendelser()) { "Systembruker mangler rollen LAGE_HENDELSER. Systembrukers roller: ${systembruker.roller}" }
        val oppgaveId =
            oppgaveGateway.opprettOppgave(søknad.fnr, JournalpostId(søknad.journalpostId), Oppgavebehov.NY_SOKNAD)
        log.info { "Opprettet oppgave med id $oppgaveId for søknad med id ${søknad.id}" }
        søknadRepo.lagre(søknad.copy(oppgaveId = oppgaveId))
    }

    override fun hentSøknad(søknadId: SøknadId): Søknad {
        return søknadRepo.hentForSøknadId(søknadId)!!
    }

    override fun hentSakIdForSoknad(søknadId: SøknadId): SakId {
        return søknadRepo.hentSakIdForSoknad(søknadId)
            ?: throw IllegalStateException("Fant ikke sak for søknad med id $søknadId")
    }

    override fun lagreAvbruttSøknad(søknad: Søknad, tx: TransactionContext) {
        søknadRepo.lagreAvbruttSøknad(søknad, tx)
    }
}
