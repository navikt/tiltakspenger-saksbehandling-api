package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.åpneBehandlingerMedKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.service.OpprettMeldekortbehandlingService.OpprettMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak

suspend fun Sak.opprettBehandlingFraKlage(
    kommando: OpprettBehandlingFraKlageKommando,
    opprettSøknadsbehandling: suspend (StartSøknadsbehandlingPåNyttKommando, Sak) -> Pair<Sak, Søknadsbehandling>,
    opprettRevurdering: suspend (StartRevurderingKommando, Sak) -> Pair<Sak, Revurdering>,
    opprettMeldekortbehandling: suspend (OpprettMeldekortbehandlingKommando, Sak) -> Pair<Sak, Meldekortbehandling>,
): Either<KanIkkeOppretteBehandlingFraKlage, Pair<Sak, AttesterbarBehandling>> {
    val klagebehandling: Klagebehandling = this.hentKlagebehandling(kommando.klagebehandlingId)
    this.åpneBehandlingerMedKlagebehandlingId(klagebehandling.id).also {
        if (it.isNotEmpty()) {
            return KanIkkeOppretteBehandlingFraKlage.FinnesÅpenBehandling(it.first().id).left()
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
        )

        is OpprettMeldekortbehandlingFraKlageKommando -> this.opprettMeldekortbehandlingFraKlage(
            kommando,
            opprettMeldekortbehandling,
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
                OpprettRevurderingFraKlageKommando.Type.OMGJØRING -> kommando.vedtakIdSomOmgjøres!!
            },
            klagebehandlingId = kommando.klagebehandlingId,
        ),
        this,
    )
}

private suspend fun Sak.opprettMeldekortbehandlingFraKlage(
    kommando: OpprettMeldekortbehandlingFraKlageKommando,
    opprettMeldekortbehandling: suspend (OpprettMeldekortbehandlingKommando, Sak) -> Pair<Sak, Meldekortbehandling>,
): Pair<Sak, Meldekortbehandling> = opprettMeldekortbehandling(
    OpprettMeldekortbehandlingKommando(
        sakId = kommando.sakId,
        kjedeId = kommando.kjedeId,
        saksbehandler = kommando.saksbehandler,
        klagebehandlingId = kommando.klagebehandlingId,
        correlationId = kommando.correlationId,
    ),
    this,
)
