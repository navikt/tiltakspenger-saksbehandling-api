package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeHenteBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeUnderkjenne

interface BehandlingService {

    /**
     * Tenkt brukt i systemkall der vi ikke skal gjøre tilgangskontroll eller sjekk på skjermet/kode6/kode7
     * Eller der vi allerede har gjort tilgangskontroll.
     */
    fun hentBehandlingForSystem(
        behandlingId: BehandlingId,
        sessionContext: SessionContext? = null,
    ): Behandling

    /**

     * Tenkt brukt for kommandoer som er trigget av saksbehandler og kjører via servicen..
     * Her gjør vi en sjekk på om saksbehandler har skjermet/kode6/kode7 tilgang dersom det er relevant.
     * Vi gjør ikke tilgangskontroll utover dette. Det må gjøres i kallene som bruker denne metoden.
     */
    suspend fun hentBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext? = null,
    ): Behandling

    /**
     * Post-mvp: Fjern denne og bruk Either i hentBehandling.
     * Tenkt brukt kall trigget av routene frem til vi implementerer Either i hentBehandling.
     * Her gjør vi en sjekk på om saksbehandler har skjermet/kode6/kode7 tilgang dersom det er relevant.
     * Vi sjekker også om saksbehandler har saksbehandler/beslutter rolle.
     */
    suspend fun hentBehandlingForSaksbehandler(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        sessionContext: SessionContext? = null,
    ): Either<no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeHenteBehandling, Behandling>

    suspend fun sendTilbakeTilSaksbehandler(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String?,
        correlationId: CorrelationId,
    ): Either<KanIkkeUnderkjenne, Behandling>

    suspend fun taBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeTaBehandling, Behandling>

    fun lagreBehandling(behandling: Behandling, tx: TransactionContext)
}
