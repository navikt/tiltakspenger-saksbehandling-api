package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT

/**
 * Avgjør selv hvilken type brev som genereres og om det er forhåndsvisning eller endelig generering.
 * @param kommando Blir kun brukt dersom saksbehandler på behandlingen forhåndsviser i tilstanden [UNDER_BEHANDLING].
 */
suspend fun Klagebehandling.genererBrev(
    kommando: KlagebehandlingBrevKommando,
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    require(resultat is Klagebehandlingsresultat.Avvist) {
        "Kan kun generere klagebrev for avvisning når formkrav er avvisning.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    if (erVedtatt) return genererBrev(genererAvvisningsbrev)
    val brevtekst = resultat.brevtekst
    val saksbehandler: String = when (status) {
        KLAR_TIL_BEHANDLING -> "-"
        UNDER_BEHANDLING -> this.saksbehandler!!
        AVBRUTT -> this.saksbehandler ?: "-"
        VEDTATT -> throw IllegalStateException("Vi håndterer denne tilstanden over.")
    }
    val erSaksbehandlerPåBehandlingen = this.erSaksbehandlerPåBehandlingen(kommando.saksbehandler)
    val tilleggstekst: Brevtekster = when (status) {
        KLAR_TIL_BEHANDLING -> brevtekst ?: Brevtekster.empty

        UNDER_BEHANDLING -> if (erSaksbehandlerPåBehandlingen) {
            kommando.brevtekster
        } else {
            brevtekst ?: Brevtekster.empty
        }

        AVBRUTT -> brevtekst ?: Brevtekster.empty

        VEDTATT -> throw IllegalStateException("Vi håndterer denne tilstanden over.")
    }
    return genererAvvisningsbrev(
        saksnummer,
        fnr,
        saksbehandler,
        tilleggstekst,
        true,
    )
}

/**
 * Kun til bruk av systemet når det skal genereres endelig brev.
 * @throws IllegalArgumentException dersom klagebehandlingen ikke er iverksatt.
 */
suspend fun Klagebehandling.genererBrev(
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    require(erVedtatt) {
        "Kan kun generere klagebrev for avvisning når klagebehandling er iverksatt.sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
    }
    return genererAvvisningsbrev(
        saksnummer,
        fnr,
        saksbehandler!!,
        (resultat as Klagebehandlingsresultat.Avvist).brevtekst!!,
        false,
    )
}
