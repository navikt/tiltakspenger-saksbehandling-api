package no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.sendForvaltningsmeldingForKlagebehandling(
    kommando: ForvaltningsmeldingKommando,
    clock: Clock,
): Either<KanIkkeSendeForvaltningsmelding, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .sendForvaltningsmelding(kommando, clock)
        .map { Pair(this.oppdaterKlagebehandling(it), it) }
}

suspend fun Sak.forhåndsvisForvaltningsmeldingForKlagebehandling(
    kommando: ForvaltningsmeldingKommando,
    generer: GenererKlageForvaltningsmelding,
    clock: Clock,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .forhåndsvisForvaltningsmelding(kommando, generer, clock)
}
