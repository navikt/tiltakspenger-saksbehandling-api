package no.nav.tiltakspenger.saksbehandling.behandling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.DigitalsøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.søknad.Digitalsøknad

class DigitalsøknadService(
    private val digitalsøknadRepo: DigitalsøknadRepo,
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
) {
    private val log = KotlinLogging.logger {}

    /** Skal i førsteomgang kun brukes til digitale søknader. Dersom en saksbehandler skal registere en papirsøknad må vi ha en egen funksjon som sjekker tilgang.*/
    fun nySøknad(søknad: Digitalsøknad) {
        sessionFactory.withTransactionContext { tx ->
            digitalsøknadRepo.lagre(søknad, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = søknad.sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }
        log.info { "Lagret søknad med id ${søknad.id}" }
    }

    fun lagreAvbruttSøknad(søknad: Digitalsøknad, tx: TransactionContext) {
        digitalsøknadRepo.lagreAvbruttSøknad(søknad, tx)
    }
}
