package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.åpneRammebehandlingerMedKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.sak.Sak

suspend fun Sak.opprettRammebehandlingFraKlage(
    kommando: OpprettRammebehandlingFraKlageKommando,
    opprettSøknadsbehandling: suspend (StartSøknadsbehandlingPåNyttKommando, Sak) -> Pair<Sak, Søknadsbehandling>,
    opprettRevurdering: suspend (StartRevurderingKommando, Sak) -> Pair<Sak, Revurdering>,
): Either<KanIkkeOppretteRammebehandlingFraKlage, Pair<Sak, Rammebehandling>> {
    val klagebehandling: Klagebehandling = this.hentKlagebehandling(kommando.klagebehandlingId)
    this.åpneRammebehandlingerMedKlagebehandlingId(klagebehandling.id).also {
        if (it.isNotEmpty()) {
            return KanIkkeOppretteRammebehandlingFraKlage.FinnesÅpenRammebehandling(it.first().id).left()
        }
    }
    return when (kommando) {
        is OpprettSøknadsbehandlingFraKlageKommando -> this.opprettSøknadsbehandlingFraKlage(
            kommando = kommando,
            opprettSøknadsbehandling = opprettSøknadsbehandling,
        )

        is OpprettRevurderingFraKlageKommando -> this.opprettRevurderingFraKlage(
            kommando = kommando,
            opprettRevurdering = opprettRevurdering,
            vedtakIdSomOmgjøres = klagebehandling.formkrav.vedtakDetKlagesPå,
        )
    }.right()
}

private suspend fun Sak.opprettSøknadsbehandlingFraKlage(
    kommando: OpprettSøknadsbehandlingFraKlageKommando,
    opprettSøknadsbehandling: suspend (StartSøknadsbehandlingPåNyttKommando, Sak) -> Pair<Sak, Søknadsbehandling>,
): Pair<Sak, Søknadsbehandling> {
    return opprettSøknadsbehandling(
        StartSøknadsbehandlingPåNyttKommando(
            sakId = kommando.sakId,
            søknadId = kommando.søknadId,
            klagebehandlingId = kommando.klagebehandlingId,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
        this,
    )
}

private suspend fun Sak.opprettRevurderingFraKlage(
    kommando: OpprettRevurderingFraKlageKommando,
    opprettRevurdering: suspend (StartRevurderingKommando, Sak) -> Pair<Sak, Revurdering>,
    vedtakIdSomOmgjøres: VedtakId?,
): Pair<Sak, Rammebehandling> {
    return opprettRevurdering(
        StartRevurderingKommando(
            sakId = kommando.sakId,
            correlationId = kommando.correlationId,
            saksbehandler = kommando.saksbehandler,
            revurderingType = when (kommando.type) {
                OpprettRevurderingFraKlageKommando.Type.INNVILGELSE -> StartRevurderingType.INNVILGELSE
                OpprettRevurderingFraKlageKommando.Type.OMGJØRING -> StartRevurderingType.OMGJØRING
            },
            vedtakIdSomOmgjøres = when (kommando.type) {
                OpprettRevurderingFraKlageKommando.Type.INNVILGELSE -> null
                OpprettRevurderingFraKlageKommando.Type.OMGJØRING -> vedtakIdSomOmgjøres!!
            },
            klagebehandlingId = kommando.klagebehandlingId,
        ),
        this,
    )
}
