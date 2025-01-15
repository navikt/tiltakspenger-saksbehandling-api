package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeHenteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeIverksetteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppdatereTilleggstekstBrev
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppdatereVurderingsperiode
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeUnderkjenne
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterTilleggstekstBrevKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterVurderingsperiodeKommando

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
    ): Either<KanIkkeHenteBehandling, Behandling>

    suspend fun oppdaterVurderingsPeriodeForBehandling(
        kommando: OppdaterVurderingsperiodeKommando,
    ): Either<KanIkkeOppdatereVurderingsperiode, Behandling>

    suspend fun oppdaterTilleggstekstBrevPåBehandling(
        kommando: OppdaterTilleggstekstBrevKommando,
    ): Either<KanIkkeOppdatereTilleggstekstBrev, Behandling>

    suspend fun sendTilBeslutter(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeSendeTilBeslutter, Behandling>

    suspend fun sendTilbakeTilSaksbehandler(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        begrunnelse: String,
        correlationId: CorrelationId,
    ): Either<KanIkkeUnderkjenne, Behandling>

    suspend fun iverksett(
        behandlingId: BehandlingId,
        beslutter: Saksbehandler,
        correlationId: CorrelationId,
        sakId: SakId,
    ): Either<KanIkkeIverksetteBehandling, Behandling>

    suspend fun taBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeTaBehandling, Behandling>
}
