package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.saksbehandlerFyllerUtMeldeperiodeDager
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilSendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak

suspend fun TestApplicationContext.nyOpprettetMeldekortbehandling(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
): Pair<Sak, MeldekortBehandling> {
    return meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
        kjedeId = kjedeId,
        sakId = sakId,
        saksbehandler = saksbehandler,
    ).getOrFail()
}

suspend fun TestApplicationContext.oppdatertMeldekortbehandling(
    sakId: SakId,
    meldekortId: MeldekortId,
    saksbehandler: Saksbehandler = saksbehandler(),
    dager: OppdaterMeldekortKommando.Dager = saksbehandlerFyllerUtMeldeperiodeDager(
        meldekortContext.meldekortBehandlingRepo.hent(
            meldekortId,
        )!!.meldeperiode,
    ),
    begrunnelse: String? = null,
    fritekstTilVedtaksbrev: String? = null,
    correlationId: CorrelationId = CorrelationId.generate(),
): Pair<Sak, MeldekortBehandling> {
    return meldekortContext.oppdaterMeldekortService.oppdaterMeldekort(
        kommando = OppdaterMeldekortKommando(
            sakId = sakId,
            meldekortId = meldekortId,
            saksbehandler = saksbehandler,
            dager = dager,
            begrunnelse = begrunnelse?.let { Begrunnelse.createOrThrow(it) },
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev?.let { FritekstTilVedtaksbrev.createOrThrow(it) },
            correlationId = correlationId,
        ),
    ).getOrFail()
}

suspend fun TestApplicationContext.meldekortbehandlingKlarTilBeslutning(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
): Pair<Sak, MeldekortBehandletManuelt> {
    val (_, opprettet) = nyOpprettetMeldekortbehandling(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
    )

    return meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
        opprettet.tilSendMeldekortTilBeslutterKommando(saksbehandler),
    ).getOrFail()
}

suspend fun TestApplicationContext.meldekortbehandlingUnderBeslutning(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Pair<Sak, MeldekortBehandling> {
    val (_, meldekortbehandling) = meldekortbehandlingKlarTilBeslutning(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
    )

    return meldekortContext.taMeldekortBehandlingService.taMeldekortBehandling(
        sakId = sakId,
        meldekortId = meldekortbehandling.id,
        saksbehandler = beslutter,
    )
}

suspend fun TestApplicationContext.meldekortbehandlingIverksatt(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Pair<Sak, MeldekortBehandling> {
    val (_, meldekortbehandling) = meldekortbehandlingUnderBeslutning(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

    return meldekortContext.iverksettMeldekortService.iverksettMeldekort(
        IverksettMeldekortKommando(
            meldekortId = meldekortbehandling.id,
            sakId = sakId,
            beslutter = beslutter,
            correlationId = CorrelationId.generate(),
        ),
    ).getOrFail()
}
