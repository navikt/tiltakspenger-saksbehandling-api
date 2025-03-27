package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.statistikk.stønad

import java.time.LocalDate
import java.util.UUID

// Dette er bare et eksempel. Legg til flere felt når vi vet hva som skal være med
data class StatistikkStønadDTO(
    val id: UUID,
    val brukerId: String,

    val sakId: String,
    val saksnummer: String,
    val resultat: String,
    val sakDato: LocalDate,
    val sakFraDato: LocalDate,
    val sakTilDato: LocalDate,
    val ytelse: String,

    val søknadId: String?,
    val søknadDato: LocalDate?,
    val søknadFraDato: LocalDate?,
    val søknadTilDato: LocalDate?,

    val vedtakId: String,
    val vedtaksType: String,
    val vedtakDato: LocalDate,
    val vedtakFom: LocalDate,
    val vedtakTom: LocalDate,
)
