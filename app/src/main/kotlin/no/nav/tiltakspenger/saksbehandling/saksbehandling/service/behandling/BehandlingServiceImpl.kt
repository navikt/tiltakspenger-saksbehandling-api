package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeHenteBehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService
import java.time.Clock

class BehandlingServiceImpl(
    private val behandlingRepo: BehandlingRepo,
    private val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val clock: Clock,
) : BehandlingService {
    val logger = KotlinLogging.logger { }

    override fun hentBehandlingForSystem(
        behandlingId: BehandlingId,
        sessionContext: SessionContext?,
    ): Behandling = behandlingRepo.hent(behandlingId, sessionContext)

    override suspend fun hentBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext?,
    ): Behandling {
        require(saksbehandler.erSaksbehandlerEllerBeslutter()) { "Saksbehandler må ha rollen SAKSBEHANDLER eller BESLUTTER" }
        sjekkTilgang(behandlingId, saksbehandler, correlationId)

        val behandling = hentBehandlingForSystem(behandlingId, sessionContext)
        return behandling
    }

    override suspend fun hentBehandlingForSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext?,
    ): Either<KanIkkeHenteBehandling, Behandling> {
        if (!saksbehandler.erSaksbehandler()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å hente behandling" }
            return KanIkkeHenteBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }
        sjekkTilgang(behandlingId, saksbehandler, correlationId)

        val behandling = hentBehandlingForSystem(behandlingId, sessionContext)
        return behandling.right()
    }

    override suspend fun sendTilbakeTilSaksbehandler(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String,
        correlationId: CorrelationId,

    ): Either<KanIkkeUnderkjenne, Behandling> {
        if (!beslutter.erBeslutter()) {
            logger.warn { "Navident ${beslutter.navIdent} med rollene ${beslutter.roller} har ikke tilgang til å underkjenne behandlingen" }
            return KanIkkeUnderkjenne.MåVæreBeslutter.left()
        }
        val attestering =
            Attestering(
                status = Attesteringsstatus.SENDT_TILBAKE,
                begrunnelse = begrunnelse,
                beslutter = beslutter.navIdent,
                tidspunkt = nå(clock),
            )

        val behandling =
            hentBehandling(behandlingId, beslutter, correlationId).sendTilbakeTilBehandling(beslutter, attestering).also {
                sessionFactory.withTransactionContext { tx ->
                    behandlingRepo.lagre(it, tx)
                }
            }
        return behandling.right()
    }

    override suspend fun taBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeTaBehandling, Behandling> {
        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å ta behandling" }
            return KanIkkeTaBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }
        val behandling = hentBehandling(behandlingId, saksbehandler, correlationId)
        return behandling.taBehandling(saksbehandler).also {
            behandlingRepo.lagre(it)
        }.right()
    }

    override fun lagreBehandling(behandling: Behandling, tx: TransactionContext) {
        behandlingRepo.lagre(behandling, tx)
    }

    private suspend fun sjekkTilgang(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) {
        val fnr = personService.hentFnrForBehandlingId(behandlingId)
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            )
            .onLeft { underliggendeFeil ->
                sikkerlogg.error(
                    underliggendeFeil.exception ?: IllegalArgumentException("Trigger en stacktrace for debugging"),
                ) { "Feil ved sjekk av tilgang til person. BehandlingId: $behandlingId. CorrelationId: $correlationId. body: ${underliggendeFeil.body}, status: ${underliggendeFeil.status}" }
                throw IkkeFunnetException("Feil ved sjekk av tilgang til person. BehandlingId: $behandlingId. CorrelationId: $correlationId. Feiltype: ${underliggendeFeil::class.simpleName} Se sikkerlogg for mer context")
            }
            .onRight { if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person") }
    }
}
