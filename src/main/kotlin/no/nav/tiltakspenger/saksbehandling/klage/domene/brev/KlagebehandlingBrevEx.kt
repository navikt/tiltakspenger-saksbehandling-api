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
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.MOTTATT_FRA_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OPPRETTHOLDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSENDT
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.OVERSEND_FEILET
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.VEDTATT
import java.time.LocalDate

/**
 * Avgjør selv hvilken type brev som genereres og om det er forhåndsvisning eller endelig generering.
 * @param kommando Blir kun brukt dersom saksbehandler på behandlingen forhåndsviser i tilstanden [UNDER_BEHANDLING].
 */
suspend fun Klagebehandling.genererBrev(
    kommando: KlagebehandlingBrevKommando,
    genererAvvisningsbrev: GenererKlageAvvisningsbrev,
    genererKlageInnstillingsbrev: GenererKlageInnstillingsbrev,
    // til bruk for innstillingsbrev
    vedtaksdato: LocalDate?,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    require(resultat is Klagebehandlingsresultat.Avvist || resultat is Klagebehandlingsresultat.Opprettholdt) {
        """
            Kan kun generere klagebrev dersom;
                - Klagebehandlingen har formkravene sine avvist
                - Klagebehandlingen har formkravene sine til vurdering
            Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id
        """.trimIndent()
    }
    if (skalGenerereBrevKunFraBehandling()) return genererBrev(genererAvvisningsbrev, genererKlageInnstillingsbrev, vedtaksdato)

    val brevtekst = resultat.brevtekst
    val saksbehandler: String = when (status) {
        KLAR_TIL_BEHANDLING -> "-"
        UNDER_BEHANDLING -> this.saksbehandler!!
        AVBRUTT -> this.saksbehandler ?: "-"
        VEDTATT, OPPRETTHOLDT, OVERSENDT, OVERSEND_FEILET, FERDIGSTILT, MOTTATT_FRA_KLAGEINSTANS, OMGJØRING_ETTER_KLAGEINSTANS -> throw IllegalStateException("Vi håndterer denne tilstanden over.")
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
            vedtaksdato!!,
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
    // til bruk for innstillingsbrev
    vedtaksdato: LocalDate?,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    require(skalGenerereBrevKunFraBehandling()) {
        """
            Kan kun generere endelig klagebrev dersom;
                - klagen er avvist og klagebehandlingen er vedtatt
                - klagen er opprettholdt og oversendt til klageinstansen
            Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id
        """.trimIndent()
    }
    return when (resultat) {
        is Klagebehandlingsresultat.Avvist -> {
            require(status == VEDTATT) {
                "Ved generering av endelig avvisningsbrev må klagebehandlingen være vedtatt. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
            }
            genererAvvisningsbrev(
                saksnummer,
                fnr,
                saksbehandler!!,
                resultat.brevtekst!!,
                false,
            )
        }

        is Klagebehandlingsresultat.Opprettholdt -> {
            require(status in listOf(OPPRETTHOLDT, OVERSENDT, FERDIGSTILT, MOTTATT_FRA_KLAGEINSTANS)) {
                "Ved generering av endelig innstillingsbrev må klagebehandlingen være oversendt til klageinstansen. sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id"
            }
            genererKlageInnstillingsbrev(
                saksnummer,
                fnr,
                saksbehandler!!,
                resultat.brevtekst!!,
                false,
                this.formkrav.innsendingsdato,
                vedtaksdato!!,
            )
        }

        is Klagebehandlingsresultat.Omgjør, null -> throw IllegalStateException("Ingen resultat og omgjør er ikke en gyldig tilstand for klagebehandlingsresultat ved generering av brev. Feilen skjedde for sakId=$sakId, saksnummer:$saksnummer, klagebehandlingId=$id")
    }
}
