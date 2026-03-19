package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

class RammebehandlingService(
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
) {
    val logger = KotlinLogging.logger { }

    fun hentSakOgRammebehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
    ): Pair<Sak, Rammebehandling> {
        val sak = sakService.hentForSakId(sakId)
        val behandling = sak.hentRammebehandling(behandlingId)

        requireNotNull(behandling) {
            "Fant ikke behandling $behandlingId på sak $sakId"
        }

        return Pair(sak, behandling)
    }

    suspend fun underkjennBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String?,
    ): Either<KanIkkeUnderkjenne, Pair<Sak, Rammebehandling>> {
        val (sak, rammebehandling) = hentSakOgRammebehandling(sakId, behandlingId)

        val nonBlankBegrunnelse = Either.catch { begrunnelse?.toNonBlankString() }.getOrElse {
            return KanIkkeUnderkjenne.ManglerBegrunnelse.left()
        }

        val attestering = Attestering(
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = nonBlankBegrunnelse,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )

        // Denne validerer saksbehandler
        return rammebehandling
            .underkjenn(beslutter, attestering, clock)
            .let { (oppdatertRammebehandling, statistikkhendelser) ->
                val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)

                lagreMedStatistikk(behandling = oppdatertRammebehandling, statistikkhendelser = statistikkhendelser)

                oppdatertSak to oppdatertRammebehandling
            }.right()
    }

    /**
     * Denne gjør ingen tilgangskontroll. Ansvaret ligger hos kalleren.
     */
    suspend fun lagreMedStatistikk(
        behandling: Rammebehandling,
        statistikkhendelser: Statistikkhendelser,
        tx: TransactionContext? = null,
    ) {
        val statistikkDto = statistikkService.generer(statistikkhendelser)
        sessionFactory.withTransactionContext(tx) { tx ->
            rammebehandlingRepo.lagre(behandling, tx)
            statistikkService.lagre(statistikkDto, tx)
        }
    }
}
