package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.iverksett.IverksettMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilSendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

suspend fun TestApplicationContext.nyOpprettetMeldekortbehandling(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
): Pair<Sak, Meldekortbehandling> {
    return meldekortContext.opprettMeldekortbehandlingService.opprettBehandling(
        kjedeId = kjedeId,
        sakId = sakId,
        saksbehandler = saksbehandler,
    ).getOrFail()
}

suspend fun TestApplicationContext.oppdatertMeldekortbehandling(
    sakId: SakId,
    saksbehandler: Saksbehandler = saksbehandler(),
    kjedeId: MeldeperiodeKjedeId,
    clock: Clock = this.clock,
): Pair<Sak, Meldekortbehandling> {
    val (_, opprettet) = nyOpprettetMeldekortbehandling(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
    )

    return meldekortContext.oppdaterMeldekortbehandlingService.oppdaterMeldekort(
        kommando = opprettet.tilOppdaterMeldekortKommando(saksbehandler),
        clock = clock,
    ).getOrFail()
}

suspend fun TestApplicationContext.meldekortbehandlingKlarTilBeslutning(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
    clock: Clock = this.clock,
): Pair<Sak, MeldekortbehandlingManuell> {
    val (_, opprettet) = oppdatertMeldekortbehandling(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
    )

    return meldekortContext.sendMeldekortbehandlingTilBeslutterService.sendMeldekortTilBeslutter(
        opprettet.tilSendMeldekortTilBeslutterKommando(saksbehandler),
        clock,
    ).getOrFail()
}

suspend fun TestApplicationContext.meldekortbehandlingUnderBeslutning(
    sakId: SakId,
    kjedeId: MeldeperiodeKjedeId,
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Pair<Sak, Meldekortbehandling> {
    val (_, meldekortbehandling) = meldekortbehandlingKlarTilBeslutning(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
    )

    return meldekortContext.taMeldekortbehandlingService.taMeldekortbehandling(
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
): Pair<Sak, Meldekortbehandling> {
    val (_, meldekortbehandling) = meldekortbehandlingUnderBeslutning(
        sakId = sakId,
        kjedeId = kjedeId,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

    return meldekortContext.iverksettMeldekortbehandlingService.iverksettMeldekort(
        IverksettMeldekortbehandlingKommando(
            meldekortId = meldekortbehandling.id,
            sakId = sakId,
            beslutter = beslutter,
            correlationId = CorrelationId.generate(),
        ),
    ).getOrFail()
}
