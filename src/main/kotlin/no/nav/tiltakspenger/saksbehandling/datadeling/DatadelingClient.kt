package no.nav.tiltakspenger.saksbehandling.datadeling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakDb
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Venstresiden er [HttpKlientError] fra `tiltakspenger-libs:httpklient`.
 * Vi gjenbruker feiltypen fra libs i stedet for en egen domenefeil fordi konsumenten ([SendTilDatadelingService]) kun bryr seg om Right vs. Left, og fordi feiltypen selv bærer all HTTP-kontekst (status, rå request/respons, throwable) som trengs for logging.
 * Selve feilloggingen gjøres én gang av konsumenten via [no.nav.tiltakspenger.libs.httpklient.loggFeil], som kombinerer HTTP-konteksten fra feiltypen med domenekonteksten konsumenten har.
 */
interface DatadelingClient {

    suspend fun send(
        rammevedtak: Rammevedtak,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit>

    suspend fun send(
        behandling: AttesterbarBehandling,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit>

    suspend fun send(
        meldeperioder: List<Meldeperiode>,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit>

    suspend fun send(
        meldekortvedtak: Meldekortvedtak,
        differansePerKjede: Map<MeldeperiodeKjedeId, Int>?,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit>

    suspend fun send(
        sakDb: SakDb,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit>
}
