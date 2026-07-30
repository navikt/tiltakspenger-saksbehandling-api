package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.datadeling.DatadelingClient
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakDb
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Registrerer hva som ble sendt til datadeling, slik at tester kan assertere på sideeffekten av [no.nav.tiltakspenger.saksbehandling.datadeling.SendTilDatadelingService].
 */
class DatadelingFakeKlient : DatadelingClient {

    val sendteMeldekortvedtak = ConcurrentLinkedQueue<VedtakId>()
    val sendteRammevedtak = ConcurrentLinkedQueue<VedtakId>()
    val sendteBehandlinger = ConcurrentLinkedQueue<BehandlingId>()
    val sendteSaker = ConcurrentLinkedQueue<SakId>()

    /** Jobben sender meldeperiodene for én sak av gangen, så hvert element er ett kall. */
    val sendteMeldeperioder = ConcurrentLinkedQueue<List<MeldeperiodeId>>()

    override suspend fun send(
        rammevedtak: Rammevedtak,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        sendteRammevedtak.add(rammevedtak.id)
        return Unit.right()
    }

    override suspend fun send(
        behandling: AttesterbarBehandling,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        sendteBehandlinger.add(behandling.id)
        return Unit.right()
    }

    override suspend fun send(
        meldeperioder: List<Meldeperiode>,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        sendteMeldeperioder.add(meldeperioder.map { it.id })
        return Unit.right()
    }

    override suspend fun send(
        meldekortvedtak: Meldekortvedtak,
        differansePerKjede: Map<MeldeperiodeKjedeId, Int>?,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        sendteMeldekortvedtak.add(meldekortvedtak.id)
        return Unit.right()
    }

    override suspend fun send(
        sakDb: SakDb,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, Unit> {
        sendteSaker.add(sakDb.id)
        return Unit.right()
    }
}
