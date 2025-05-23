package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
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
    suspend fun hentBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext? = null,
    ): Behandling {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        val behandling = behandlingRepo.hent(behandlingId, sessionContext)
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, behandling.fnr, correlationId)
        return behandling
    }

    /**
     * Sjekker om saksbehandler har tilgang til personen og har rollen BESLUTTER.
     */
    suspend fun sendTilbakeTilSaksbehandler(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String?,
        correlationId: CorrelationId,
    ): Either<KanIkkeUnderkjenne, Behandling> {
        // Sjekker om beslutter har tilgang til personen og har rollen SAKSBEHANDLER eller BESLUTTER.
        val behandling = hentBehandling(behandlingId, beslutter, correlationId)

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
        return behandling.sendTilbakeTilBehandling(beslutter, attestering).also {
            val statistikk = statistikkSakService.genererStatistikkForUnderkjennBehandling(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }.right()
    }

    /**
     * Denne gjør ingen tilgangskontroll. Ansvaret ligger hos kalleren.
     */
    fun lagreBehandling(behandling: Behandling, tx: TransactionContext) {
        behandlingRepo.lagre(behandling, tx)
    }
}
