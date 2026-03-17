package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageInnstillingsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

suspend fun Sak.forhåndsvisKlagebrev(
    kommando: KlagebehandlingBrevKommando,
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    genererKlageInnstillingsbrev: GenererKlageInnstillingsbrev,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    val klage = this.hentKlagebehandling(kommando.klagebehandlingId)
    val vedtak = this.vedtaksliste.alle.singleOrNullOrThrow { it.id == klage.formkrav.vedtakDetKlagesPå }

    return klage.genererBrev(
        kommando = kommando,
        genererAvvisningsbrev = genererAvvisningsbrev,
        genererKlageInnstillingsbrev = genererKlageInnstillingsbrev,
        vedtaksdato = vedtak?.opprettet?.toLocalDate(),
    )
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
