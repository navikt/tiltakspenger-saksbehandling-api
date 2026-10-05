package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import java.time.Clock

class StartRevurderingService(
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
    ): Either<KanIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
        val sak = sakService.hentForSakId(kommando.sakId)
        return startRevurdering(kommando, sak)
    }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
        sak: Sak,
    ): Either<KanIkkeStarteRevurdering, Pair<Sak, Revurdering>> {
        val forberedt = forberedRevurdering(kommando, sak).getOrElse { return it.left() }
        sessionFactory.withTransactionContext { tx -> lagre(forberedt, tx) }
        return Pair(forberedt.sak, forberedt.revurdering).right()
    }

    /**
     * Henter saksopplysninger og bygger revurderingen og statistikken, uten å lagre noe.
     * Lar kalleren lagre revurderingen med [lagre] i samme transaksjon som egne endringer, uten å holde transaksjonen åpen under oppslagene.
     */
    suspend fun forberedRevurdering(
        kommando: StartRevurderingKommando,
        sak: Sak,
    ): Either<KanIkkeStarteRevurdering, ForberedtRevurdering> {
        val (oppdatertSak, revurdering) = sak.startRevurdering(
            kommando = kommando,
            clock = clock,
            hentSaksopplysninger = { fnr, correlationId, tiltaksdeltakelserDetErSøktTiltakspengerFor, aktuelleTiltaksdeltakelserForBehandlingen, inkluderOverlappendeTiltaksdeltakelserDetErSøktOm, sakId ->
                hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
                    fnr = fnr,
                    correlationId = correlationId,
                    tiltaksdeltakelserDetErSøktTiltakspengerFor = tiltaksdeltakelserDetErSøktTiltakspengerFor,
                    aktuelleTiltaksdeltakelserForBehandlingen = aktuelleTiltaksdeltakelserForBehandlingen,
                    inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = inkluderOverlappendeTiltaksdeltakelserDetErSøktOm,
                    sakId = sakId,
                    saksnummer = sak.saksnummer,
                )
            },
        ).getOrElse { return it.left() }

        val statistikk = statistikkService.generer(
            Statistikkhendelser(
                revurdering.genererSaksstatistikk(
                    hendelse = StatistikkhendelseType.OPPRETTET_REVURDERING,
                ),
            ),
        )

        return ForberedtRevurdering(oppdatertSak, revurdering, statistikk).right()
    }

    fun lagre(forberedt: ForberedtRevurdering, tx: TransactionContext) {
        rammebehandlingRepo.lagre(forberedt.revurdering, tx)
        statistikkService.lagre(forberedt.statistikk, tx)
    }
}

/**
 * En revurdering som er bygget, men ikke lagret.
 * Lagres med [StartRevurderingService.lagre].
 */
class ForberedtRevurdering(
    val sak: Sak,
    val revurdering: Revurdering,
    val statistikk: StatistikkDTO,
)
