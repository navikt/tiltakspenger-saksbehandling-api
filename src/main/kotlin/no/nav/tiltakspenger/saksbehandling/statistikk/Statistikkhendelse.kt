package no.nav.tiltakspenger.saksbehandling.statistikk

/**
 * En endring som skal gi statistikk til datavarehuset (DVH).
 * Domenefunksjonene returnerer [Statistikkhendelser]; servicen gjør dem om til rader med [StatistikkService.generer] før transaksjonen, og lagrer dem med [StatistikkService.lagre] i samme transaksjon som endringen.
 * Tabellene, konsumentene og reglene for endringer står i `docs/statistikk/README.md`.
 */
sealed interface Statistikkhendelse
