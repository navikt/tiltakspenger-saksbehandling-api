package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeStarteSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class StartSøknadsbehandlingService(
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val oppdaterSaksopplysningerService: OppdaterSaksopplysningerService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
) {

    val logger = KotlinLogging.logger {}

    suspend fun startSøknadsbehandling(
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeStarteSøknadsbehandling, Søknadsbehandling> {
        krevSaksbehandlerRolle(saksbehandler)
        // Denne sjekker tilgang til person og rollene SAKSBEHANDLER eller BESLUTTER.
        val sak = sakService.hentForSakIdEllerKast(sakId, saksbehandler, correlationId)

        val hentSaksopplysninger: suspend (Periode) -> Saksopplysninger = { saksopplysningsperiode: Periode ->
            oppdaterSaksopplysningerService.hentSaksopplysningerFraRegistre(
                fnr = sak.fnr,
                correlationId = correlationId,
                saksopplysningsperiode = saksopplysningsperiode,
            )
        }
        val behandling = Søknadsbehandling.opprett(
            sakId = sakId,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            søknad = sak.soknader.single { it.id == søknadId },
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        ).getOrElse { return KanIkkeStarteSøknadsbehandling.OppretteBehandling(it).left() }

        val statistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = behandling,
            søknadId = søknadId,
        )

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }

        return behandling.right()
    }
}
