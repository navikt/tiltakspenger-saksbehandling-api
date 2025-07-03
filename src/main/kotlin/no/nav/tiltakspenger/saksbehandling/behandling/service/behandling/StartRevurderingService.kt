package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class StartRevurderingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val saksopplysningerService: OppdaterSaksopplysningerService,
    private val navkontorService: NavkontorService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
    ): Pair<Sak, Revurdering> {
        val (sakId, correlationId, saksbehandler) = kommando

        // Denne sjekker tilgang til person og at saksbehandler har rollen SAKSBEHANDLER eller BESLUTTER.
        val sak = sakService.sjekkTilgangOgHentForSakId(sakId, saksbehandler, correlationId)

        val navkontor = Either.catch {
            navkontorService.hentOppfolgingsenhet(sak.fnr)
        }.getOrElse {
            with("Kunne ikke hente navkontor for sak $sakId") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
            }
            throw it
        }

        val (oppdatertSak, revurdering) = sak.startRevurdering(
            kommando = kommando,
            clock = clock,
            navkontor = navkontor,
            hentSaksopplysninger = saksopplysningerService::hentSaksopplysningerFraRegistre,
        )

        val statistikk = statistikkSakService.genererStatistikkForRevurdering(revurdering)

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(revurdering, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
        return Pair(oppdatertSak, revurdering)
    }
}
