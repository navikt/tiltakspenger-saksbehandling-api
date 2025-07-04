package no.nav.tiltakspenger.saksbehandling.behandling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.krevLagreSoknadRollen
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

class SøknadService(
    private val søknadRepo: SøknadRepo,
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
) {
    private val log = KotlinLogging.logger {}

    /** Skal i førsteomgang kun brukes til digitale søknader. Dersom en saksbehandler skal registere en papirsøknad må vi ha en egen funksjon som sjekker tilgang.*/
    fun nySøknad(søknad: Søknad, systembruker: Systembruker) {
        krevLagreSoknadRollen(systembruker)
        sessionFactory.withTransactionContext { tx ->
            søknadRepo.lagre(søknad, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = søknad.sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }
        log.info { "Lagret søknad med id ${søknad.id}" }
    }

    fun lagreAvbruttSøknad(søknad: Søknad, tx: TransactionContext) {
        søknadRepo.lagreAvbruttSøknad(søknad, tx)
    }
}
