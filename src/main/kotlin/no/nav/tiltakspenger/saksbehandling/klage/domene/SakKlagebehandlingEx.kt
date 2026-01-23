package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.KanIkkeIverksetteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettRammebehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettRevurderingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettSøknadsbehandlingFraKlageKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KanIkkeVurdereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.VurderKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDateTime

fun Sak.leggTilKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(behandlinger = this.behandlinger.leggTilKlagebehandling(klagebehandling))
}

fun Sak.oppdaterKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(behandlinger = this.behandlinger.oppdaterKlagebehandling(klagebehandling))
}

fun Sak.hentKlagebehandling(klagebehandlingId: KlagebehandlingId): Klagebehandling {
    return this.behandlinger.hentKlagebehandling(klagebehandlingId)
}

fun Sak.oppdaterKlagebehandlingFormkrav(
    kommando: OppdaterKlagebehandlingFormkravKommando,
    journalpostOpprettet: LocalDateTime,
    clock: Clock,
): Either<KanIkkeOppdatereKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .oppdaterFormkrav(kommando, journalpostOpprettet, clock)
        .map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Pair(oppdatertSak, it)
        }
}

fun Sak.oppdaterKlagebehandlingBrevtekst(
    kommando: KlagebehandlingBrevKommando,
    clock: Clock,
): Either<KanIkkeOppdatereKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .oppdaterBrevtekst(kommando, clock)
        .map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Pair(oppdatertSak, it)
        }
}

fun Sak.avbrytKlagebehandling(
    kommando: AvbrytKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeAvbryteKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).avbryt(kommando, clock)
        .map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Pair(oppdatertSak, it)
        }
}

suspend fun Sak.forhåndsvisKlagebrev(
    kommando: KlagebehandlingBrevKommando,
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).genererBrev(
        kommando = kommando,
        genererAvvisningsbrev = genererAvvisningsbrev,
    )
}

fun Sak.iverksettKlagebehandling(
    kommando: IverksettKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeIverksetteKlagebehandling, Pair<Sak, Klagevedtak>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).iverksett(
        kommando = kommando,
        clock = clock,
    ).map {
        val klagevedtak = Klagevedtak.createFromKlagebehandling(
            clock = clock,
            klagebehandling = it,
        )
        val oppdatertSak = this.oppdaterKlagebehandling(it).leggTilKlagevedtak(klagevedtak)
        Pair(oppdatertSak, klagevedtak)
    }
}

fun Sak.vurderKlagebehandling(
    kommando: VurderKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeVurdereKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .vurder(kommando, clock)
        .map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Pair(oppdatertSak, it)
        }
}

suspend fun Sak.opprettRammebehandlingFraKlage(
    kommando: OpprettRammebehandlingFraKlageKommando,
    clock: Clock,
    opprettSøknadsbehandling: suspend (StartSøknadsbehandlingPåNyttKommando) -> Pair<Sak, Søknadsbehandling>,
    opprettRevurdering: suspend (StartRevurderingKommando) -> Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>>,
): Triple<Sak, Klagebehandling, Rammebehandling> {
    val klagebehandling: Klagebehandling = this.hentKlagebehandling(kommando.klagebehandlingId)
    val nå = nå(clock)
    require(
        this.åpneRammebehandlingerMedKlagebehandlingId(klagebehandling.id).all { nå > it.opprettet.plusSeconds(10) },
    ) {
        "Vent litt før du oppretter ny rammebehandling fra klagebehandling ${klagebehandling.id} på sak ${this.id}"
    }
    return when (kommando) {
        is OpprettSøknadsbehandlingFraKlageKommando -> opprettSøknadsbehandlingFraKlage(
            kommando = kommando,
            klagebehandling = klagebehandling,
            opprettSøknadsbehandling = opprettSøknadsbehandling,
        )

        is OpprettRevurderingFraKlageKommando -> opprettRevurderingFraKlage(
            kommando = kommando,
            klagebehandling = klagebehandling,
            opprettRevurdering = opprettRevurdering,
        )
    }
}

private suspend fun opprettSøknadsbehandlingFraKlage(
    kommando: OpprettSøknadsbehandlingFraKlageKommando,
    klagebehandling: Klagebehandling,
    opprettSøknadsbehandling: suspend (StartSøknadsbehandlingPåNyttKommando) -> Pair<Sak, Søknadsbehandling>,
): Triple<Sak, Klagebehandling, Søknadsbehandling> {
    return opprettSøknadsbehandling(
        StartSøknadsbehandlingPåNyttKommando(
            sakId = kommando.sakId,
            søknadId = kommando.søknadId,
            klagebehandlingId = kommando.klagebehandlingId,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
    ).let {
        Triple(it.first, klagebehandling, it.second)
    }
}

private suspend fun opprettRevurderingFraKlage(
    kommando: OpprettRevurderingFraKlageKommando,
    klagebehandling: Klagebehandling,
    opprettRevurdering: suspend (StartRevurderingKommando) -> Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>>,
): Triple<Sak, Klagebehandling, Rammebehandling> {
    val sakId = kommando.sakId
    return opprettRevurdering(
        StartRevurderingKommando(
            sakId = sakId,
            correlationId = kommando.correlationId,
            saksbehandler = kommando.saksbehandler,
            revurderingType = when (kommando.type) {
                OpprettRevurderingFraKlageKommando.Type.INNVILGELSE -> RevurderingType.INNVILGELSE
                OpprettRevurderingFraKlageKommando.Type.OMGJØRING -> RevurderingType.OMGJØRING
            },
            vedtakIdSomOmgjøres = when (kommando.type) {
                OpprettRevurderingFraKlageKommando.Type.INNVILGELSE -> null
                OpprettRevurderingFraKlageKommando.Type.OMGJØRING -> kommando.vedtakIdSomOmgjøres!!
            },
            klagebehandlingId = kommando.klagebehandlingId,
        ),
    ).map {
        Triple(it.first, klagebehandling, it.second)
    }.getOrElse {
        // TODO jah - bedre feilbehandling
        throw IllegalStateException("Kunne ikke opprette revurdering fra klagebehandling ${klagebehandling.id} på sak $sakId: $it")
    }
}

fun Sak.åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Rammebehandling> {
    return this.behandlinger.åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId)
}
