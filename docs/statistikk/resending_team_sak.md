# Resending og retting av rader til Team Sak (DVH)

Bakgrunnen står i [README.md](README.md): Team Sak henter bare rader med `id` høyere enn den største de har, og beholder én rad per `behandlingid` + `endrettidspunkt`, den med nyeste `teknisktidspunkt`.
Endringer i eksisterende rader ser de derfor ikke.
For å rette en hendelse setter du inn en ny rad med samme `behandlingid` og `endrettidspunkt`, de rettede verdiene og et nytt `teknisktidspunkt`.
Den nye raden får ny `id` fra sekvensen og erstatter den gamle hos Team Sak.

Team Saks forklaring av funksjonell, teknisk og lastet tid: <https://confluence.adeo.no/spaces/DVH/pages/459904637/Funksjonell+tid+teknisk+tid+og+lastet+tids+rolle+i+modell>

Vi har ikke noe ferdig funksjonalitet for resending, men denne SQL-en er ganske effektiv og har blitt brukt for resending og patch av behandlinger der `soknadsformat` og `behandlingtype` måtte endres:

```sql
INSERT INTO statistikk_sak (
    sak_id,
    saksnummer,
    behandlingid,
    relatertbehandlingid,
    fnr,
    mottatt_tidspunkt,
    registrerttidspunkt,
    ferdigbehandlettidspunkt,
    vedtaktidspunkt,
    utbetalttidspunkt,
    endrettidspunkt,
    soknadsformat,
    forventetoppstarttidspunkt,
    teknisktidspunkt,
    sakytelse,
    behandlingtype,
    behandlingstatus,
    behandlingresultat,
    resultatbegrunnelse,
    behandlingmetode,
    opprettetav,
    saksbehandler,
    ansvarligbeslutter,
    tilbakekrevingsbelop,
    funksjonellperiode_fra_og_med,
    funksjonellperiode_til_og_med,
    hendelse,
    avsender,
    versjon,
    behandling_aarsak,
    relatertfagsystem,
    sakutland,
    ansvarligenhet
)
SELECT
    sak_id,
    saksnummer,
    behandlingid,
    relatertbehandlingid,
    fnr,
    mottatt_tidspunkt,
    registrerttidspunkt,
    ferdigbehandlettidspunkt,
    vedtaktidspunkt,
    utbetalttidspunkt,
    endrettidspunkt,
    'PAPIR_SKJEMA',
    forventetoppstarttidspunkt,
    clock_timestamp(),
    sakytelse,
    'SØKNADSBEHANDLING',
    behandlingstatus,
    behandlingresultat,
    resultatbegrunnelse,
    behandlingmetode,
    opprettetav,
    saksbehandler,
    ansvarligbeslutter,
    tilbakekrevingsbelop,
    funksjonellperiode_fra_og_med,
    funksjonellperiode_til_og_med,
    hendelse,
    avsender,
    versjon,
    behandling_aarsak,
    relatertfagsystem,
    sakutland,
    ansvarligenhet
FROM statistikk_sak
WHERE behandlingid = ANY(ARRAY[
    'behandlingsid1',
    'behandlingsid2'
    ])
RETURNING *;
```

Denne setter inn en kopi av alle radene for de angitte behandlings-id-ene med oppdatert `soknadsformat`, `behandlingtype` og `teknisktidspunkt`; det nye `teknisktidspunkt` gjør at Team Sak bruker den nye raden.
