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
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
) {
    val logger = KotlinLogging.logger { }

    fun hentSakOgBehandling(
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
        val (sak, behandling) = hentSakOgBehandling(sakId, behandlingId)

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
        return behandling.underkjenn(beslutter, attestering).let {
            val oppdatertSak = sak.oppdaterRammebehandling(it)

            val statistikk = statistikkSakService.genererStatistikkForUnderkjennBehandling(it)

            lagreMedStatistikk(it, statistikk)

            oppdatertSak to it
        }.right()
    }

    /**
     * Denne gjør ingen tilgangskontroll. Ansvaret ligger hos kalleren.
     */
    fun lagreMedStatistikk(
        behandling: Rammebehandling,
        statistikk: StatistikkSakDTO,
        tx: TransactionContext? = null,
    ) {
        require(behandling.id.toString() == statistikk.behandlingId) {
            "Statistikken må tilhøre behandlingen (forventet ${behandling.id}, fikk ${statistikk.behandlingId})"
        }

        require(behandling.sistEndret == statistikk.endretTidspunkt) {
            "Statistikken må ha samme endringstidspunkt som behandlingen (forventet ${behandling.sistEndret}, fikk ${statistikk.endretTidspunkt})"
        }

        sessionFactory.withTransactionContext(tx) { tx ->
            behandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
    }
}
