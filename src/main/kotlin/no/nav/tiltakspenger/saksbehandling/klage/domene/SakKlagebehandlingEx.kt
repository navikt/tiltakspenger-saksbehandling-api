package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev.ForhåndsvisVedtaksbrevKommando
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.AvbrytKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt.KanIkkeAvbryteKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.KlagebehandlingBrevKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.OppdaterKlagebehandlingFormkravKommando
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
