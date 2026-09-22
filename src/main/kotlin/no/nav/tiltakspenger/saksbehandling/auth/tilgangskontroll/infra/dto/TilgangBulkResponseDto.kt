package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurderinger

/**
 * Wire-DTO for 207-svaret fra tilgangsmaskinens bulk-endepunkt (`/api/v1/bulk/obo`).
 *
 * Brukes kun i infra.
 * Domenet skal aldri se HTTP-statuser herfra.
 *
 * Bulkoppslaget godtar inntil 1000 personer per kall, og svarer 413 over det.
 * En person Tilgangsmaskinen ikke finner, får 204 i bulksvaret, ikke 404.
 * Svaret har flere felter enn vi modellerer her, og de ignoreres av objectMapper.
 */
data class TilgangBulkResponseDto(
    val resultater: List<TilgangResponse>,
) {
    data class TilgangResponse(
        val brukerId: String,
        val status: Int,
        val detaljer: TilgangDetaljer?,
    ) {
        /** Maskerer [brukerId] (fnr, PII) slik at den ikke havner tilfeldigvis i logger. */
        override fun toString(): String = "TilgangResponse(brukerId=*****, status=$status)"
    }

    /** [title] følger JSON-kontrakten og inneholder Tilgangsmaskinens avvisningskode, rå slik den står i svaret. */
    data class TilgangDetaljer(
        val title: String,
        val begrunnelse: String,
    )

    /**
     * Sjekkene bygger på hverandre og kjøres i rekkefølge, så `either { }` med `ensure` stopper ved første brudd.
     * Et svar vi ikke klarer å tolke er en forventet feil på Left, ikke et kast.
     *
     * Avvisningskodene vi ikke kjenner igjen, samles ved siden av vurderingene.
     * [TilgangsvurderingBulk.Avvist] har bare den kategoriserte årsaken, så den rå koden ville vært borte etter mappingen.
     * Servicen trenger den for å varsle om at koden må legges til.
     */
    fun tilTilgangsvurderinger(forventedeFnr: Set<Fnr>): Either<UgyldigBulksvar, Tilgangsvurderinger> = either {
        val vurderinger = resultater.map { resultat ->
            val fnr = ensureNotNull(Fnr.tryFromString(resultat.brukerId)) {
                UgyldigBulksvar("Tilgangsmaskinen returnerte en ugyldig brukerId.")
            }

            fnr to when (resultat.status) {
                HTTP_STATUS_TILGANG -> TilgangsvurderingBulk.Godkjent

                HTTP_STATUS_AVVIST -> {
                    val detaljer = ensureNotNull(resultat.detaljer) {
                        UgyldigBulksvar("Tilgangsmaskinen returnerte 403 uten detaljer.")
                    }
                    TilgangsvurderingBulk.Avvist(
                        årsak = tilTilgangsvurderingAvvistÅrsak(detaljer.title),
                        begrunnelse = detaljer.begrunnelse,
                    )
                }

                else -> raise(UgyldigBulksvar("Tilgangsmaskinen returnerte ukjent status ${resultat.status} i bulksvaret."))
            }
        }

        ensure(vurderinger.map { it.first }.distinct().size == vurderinger.size) {
            UgyldigBulksvar("Tilgangsmaskinen returnerte flere resultater for samme person.")
        }

        val tilgangPerFnr = vurderinger.toMap()
        ensure(tilgangPerFnr.keys == forventedeFnr) {
            UgyldigBulksvar("Tilgangsmaskinen returnerte ikke resultater for nøyaktig de etterspurte personene.")
        }

        Tilgangsvurderinger(
            perFnr = tilgangPerFnr,
            ukjenteAvvisningskoder = resultater
                .filter { it.status == HTTP_STATUS_AVVIST }
                .mapNotNull { it.detaljer?.title }
                .filter { tilTilgangsvurderingAvvistÅrsak(it) == TilgangsvurderingAvvistÅrsak.UKJENT }
                .toSet(),
        )
    }

    private companion object {
        const val HTTP_STATUS_TILGANG = 204
        const val HTTP_STATUS_AVVIST = 403
    }
}

/**
 * Et bulksvar vi ikke klarte å tolke, med en PII-fri beskrivelse av hva som var galt.
 * Typen hører til wire-DTO-en i infra, og klienten oversetter den til domenets feiltype.
 */
data class UgyldigBulksvar(val beskrivelse: String)
