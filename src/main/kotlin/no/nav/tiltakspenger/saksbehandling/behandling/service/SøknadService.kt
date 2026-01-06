package no.nav.tiltakspenger.saksbehandling.behandling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

class SøknadService(
    private val søknadRepo: SøknadRepo,
    private val sessionFactory: SessionFactory,
    private val sakService: SakService,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Skal kun brukes til digitale søknader.
     */
    fun nySøknad(søknad: Søknad) {
        sessionFactory.withTransactionContext { tx ->
            val internTiltaksdeltakelsesId = søknad.tiltak?.id?.let {
                tiltaksdeltakerRepo.hentEllerLagre(it, tx)
            }
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
