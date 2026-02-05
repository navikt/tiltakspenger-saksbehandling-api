package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class StartRevurderingService(
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
    ): Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        return startRevurdering(kommando, sak)
    }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
        sak: Sak,
    ): Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
        val (oppdatertSak, revurdering) = sak.startRevurdering(
            kommando = kommando,
            clock = clock,
            hentSaksopplysninger = { fnr, correlationId, tiltaksdeltakelserDetErSøktTiltakspengerFor, aktuelleTiltaksdeltakelserForBehandlingen, inkluderOverlappendeTiltaksdeltakelserDetErSøktOm ->
                hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                    fnr = fnr,
                    correlationId = correlationId,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
                )
            },
        ).getOrElse {
            return it.left()
        }
        val statistikk = statistikkSakService.genererStatistikkForRevurdering(revurdering)
        return sessionFactory.withTransactionContext { transactionContext ->
            sessionFactory.withTransactionContext(transactionContext) { tx ->
                rammebehandlingRepo.lagre(revurdering, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
            Pair(oppdatertSak, revurdering).right()
        }
    }
}
