package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeStarteRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KanIkkeOppdatereBrevtekstPåKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.oppdaterBrevtekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereFormkravPåKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.oppdaterFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.KanIkkeIverksetteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.KanIkkeOppretteRammebehandlingFraKlage
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
): Either<KanIkkeOppdatereFormkravPåKlagebehandling, Pair<Sak, Klagebehandling>> {
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
): Either<KanIkkeOppdatereBrevtekstPåKlagebehandling, Pair<Sak, Klagebehandling>> {
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

/**
 * Reservert for iverksetting av avviste klager.
 * For medhold/omgjøring, se [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService]
 */
fun Sak.iverksettKlagebehandling(
    kommando: IverksettKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeIverksetteKlagebehandling, Pair<Sak, Klagevedtak>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).iverksett(kommando = kommando).map {
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
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        // TODO jah: Vurder å lage et domeneobjekt som wrapper klagebehandling med rammebehandling.
        val rammebehandlingsstatus = it.rammebehandlingId?.let { this.hentRammebehandling(it) }?.status
        it.vurder(kommando, rammebehandlingsstatus, clock)
            .map {
                val oppdatertSak = this.oppdaterKlagebehandling(it)
                Pair(oppdatertSak, it)
            }
    }
}

suspend fun Sak.opprettRammebehandlingFraKlage(
    kommando: OpprettRammebehandlingFraKlageKommando,
    opprettSøknadsbehandling: suspend (StartSøknadsbehandlingPåNyttKommando, Sak) -> Pair<Sak, Søknadsbehandling>,
    opprettRevurdering: suspend (StartRevurderingKommando, Sak) -> Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>>,
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
    opprettRevurdering: suspend (StartRevurderingKommando, Sak) -> Either<KunneIkkeStarteRevurdering, Pair<Sak, Revurdering>>,
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
    ).getOrElse {
        // TODO jah - bedre feilbehandling
        throw IllegalStateException("Kunne ikke opprette revurdering fra klagebehandling ${kommando.klagebehandlingId} på sak ${kommando.sakId}: $it")
    }
}

fun Sak.åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Rammebehandling> {
    return this.behandlinger.hentÅpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId)
}
