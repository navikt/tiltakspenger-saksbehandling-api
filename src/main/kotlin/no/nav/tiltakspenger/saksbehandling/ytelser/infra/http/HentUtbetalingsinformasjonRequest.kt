package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

data class HentUtbetalingsinformasjonRequest(
    val ident: String,
    // rolle kan være "UTBETALT_TIL" eller "RETTIGHETSHAVER".
    // Se https://github.com/navikt/sokos-utbetaldata/tree/main?tab=readme-ov-file#utbetalingsoppslag
    val rolle: String = "RETTIGHETSHAVER",
    val periode: UtbetalingDto.Periode,
    // periodetype kan være UTBETALINGSPERIODE eller YTELSESPERIODE, men ytelsesperiode er en veldig tung spørring
    // som ikke anbefales å bruke
    val periodetype: String = "UTBETALINGSPERIODE",
)
