Resending av rader til Team Sak (DVH)
================

Vi deler data med Team Sak (DVH) ved å inserte nye rader i tabellen `statistikk_sak`. Se DTO-klassen `no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO` 
for lenke til grensesnitt. Selve delingen skjer via BigQuery. 

DVH fanger kun opp nye rader i tabellen, ikke oppdateringer, så endringer som man ønsker at DVH skal få med seg må
komme som nye rader. DVH bruker kombinasjonen `behandlingid` + `endrettidspunkt` for å identifisere en hendelse, så
ved f.eks. teknisk patching av data må man inserte en ny rad med samme `behandlingid` + `endrettidspunkt` og endringene
man ønsker å gjøre pr rad som skal patches. Husk å oppdatere `teknisktidspunkt`. 

Dokumentasjon: https://confluence.adeo.no/spaces/DVH/pages/459904637/Funksjonell+tid+teknisk+tid+og+lastet+tids+rolle+i+modell

Vi har ikke noe ferdig funksjonalitet for resending, men denne SQL-en er ganske effektiv og har blitt brukt for resending og patch 
av behandlinger der `soknadsformat` og `behandlingtype` måtte endres: 

```
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
    NOW(),
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

Denne inserter en kopi av alle rader for de angitte behandlingId-ene med oppdatert `soknadsformat`, `behandlingtype` og 
`teknisktidspunkt` (sistnevnte er viktig for at DVH skal fange opp endringen). 
