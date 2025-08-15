package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
) {
    val logger = KotlinLogging.logger { }

    /**
     * Sjekker om saksbehandler har tilgang til personen og har rollen SAKSBEHANDLER eller BESLUTTER.
     */
    suspend fun hentSakOgBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Pair<Sak, Behandling> {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId)
        val behandling = sak.hentBehandling(behandlingId)

        requireNotNull(behandling) {
            "Fant ikke behandling $behandlingId på sak $sakId"
        }

        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, behandling.fnr, correlationId)

        return Pair(sak, behandling)
    }

    /**
     * Sjekker om saksbehandler har tilgang til personen og har rollen BESLUTTER.
     */
    suspend fun sendTilbakeTilSaksbehandler(
        sakId: SakId,
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String?,
        correlationId: CorrelationId,
    ): Either<KanIkkeUnderkjenne, Pair<Sak, Behandling>> {
        val (sak, behandling) = hentSakOgBehandling(sakId, behandlingId, beslutter, correlationId)

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
        return behandling.sendTilbakeTilBehandling(beslutter, attestering).let {
            val statistikk = statistikkSakService.genererStatistikkForUnderkjennBehandling(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }

            Pair(sak, it)
        }.right()
    }

    /**
     * Denne gjør ingen tilgangskontroll. Ansvaret ligger hos kalleren.
     */
    fun lagreMedStatistikk(
        behandling: Behandling,
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
