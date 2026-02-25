package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.GenererKlageInnstillingsbrev
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.FERDIGSTILT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT

/**
 * Avgjør selv hvilken type brev som genereres og om det er forhåndsvisning eller endelig generering.
 * @param kommando Blir kun brukt dersom saksbehandler på behandlingen forhåndsviser i tilstanden [UNDER_BEHANDLING].
 */
suspend fun Klagebehandling.genererBrev(
    kommando: KlagebehandlingBrevKommando,
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    genererKlageInnstillingsbrev: GenererKlageInnstillingsbrev,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    require(resultat is Klagebehandlingsresultat.Avvist || resultat is Klagebehandlingsresultat.Opprettholdt) {
        """
            Kan kun generere klagebrev dersom;
                - Klagebehandlingen har formkravene sine avvist
                - Klagebehandlingen har formkravene sine til vurdering
            Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id
        """.trimIndent()
    }
    if (skalGenerereBrevKunFraBehandling()) return genererBrev(genererAvvisningsbrev, genererKlageInnstillingsbrev)

    val brevtekst = resultat.brevtekst
    val saksbehandler: String = when (status) {
        KLAR_TIL_BEHANDLING -> "-"
        UNDER_BEHANDLING -> this.saksbehandler!!
        AVBRUTT -> this.saksbehandler ?: "-"
        VEDTATT, OPPRETTHOLDT, OVERSENDT, FERDIGSTILT -> throw IllegalStateException("Vi håndterer denne tilstanden over.")
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
    }

    return when (resultat) {
        is Klagebehandlingsresultat.Avvist -> genererAvvisningsbrev(
            saksnummer,
            fnr,
            saksbehandler,
            tilleggstekst,
            true,
        )

        is Klagebehandlingsresultat.Opprettholdt -> genererKlageInnstillingsbrev(
            saksnummer,
            fnr,
            saksbehandler,
            tilleggstekst,
            true,
            this.formkrav.innsendingsdato,
        )
    }
}

/**
 * Kun til bruk av systemet når det skal genereres endelig brev.
 * @throws IllegalArgumentException dersom klagebehandlingen ikke er iverksatt.
 */
suspend fun Klagebehandling.genererBrev(
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    genererKlageInnstillingsbrev: GenererKlageInnstillingsbrev,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    require(skalGenerereBrevKunFraBehandling()) {
        """
            Kan kun generere endelig klagebrev dersom;
                - klagen er avvist og klagebehandlingen er vedtatt
                - klagen er opprettholdt og oversendt til klageinstansen
            Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id
        """.trimIndent()
    }

    return when (status) {
        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        AVBRUTT,
        -> throw IllegalStateException("Ved generering av endelig brev må klagebehandlingen enten være vedtatt eller oversendt. Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id")

        VEDTATT -> genererAvvisningsbrev(
            saksnummer,
            fnr,
            saksbehandler!!,
            (resultat as Klagebehandlingsresultat.Avvist).brevtekst!!,
            false,
        )

        OPPRETTHOLDT, OVERSENDT, FERDIGSTILT -> genererKlageInnstillingsbrev(
            saksnummer,
            fnr,
            saksbehandler!!,
            (resultat as Klagebehandlingsresultat.Opprettholdt).brevtekst!!,
            false,
            this.formkrav.innsendingsdato,
        )
    }
}
