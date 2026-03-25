INSERT INTO statistikk_sak (
    sak_id, saksnummer, behandlingid, relatertbehandlingid, fnr,
    mottatt_tidspunkt, registrerttidspunkt, ferdigbehandlettidspunkt,
    vedtaktidspunkt, utbetalttidspunkt, endrettidspunkt, soknadsformat,
    forventetoppstarttidspunkt, teknisktidspunkt, sakytelse, behandlingtype,
    behandlingstatus, behandlingresultat, resultatbegrunnelse, behandlingmetode,
    opprettetav, saksbehandler, ansvarligbeslutter, tilbakekrevingsbelop,
    funksjonellperiode_fra_og_med, funksjonellperiode_til_og_med, hendelse,
    avsender, versjon, behandling_aarsak, relatertfagsystem, sakutland,
    ansvarligenhet
)
SELECT
    s.sak_id, s.saksnummer, s.behandlingid,
    (kb.formkrav->>'behandlingDetKlagesPå')::varchar AS relatertbehandlingid,
    s.fnr, s.mottatt_tidspunkt, s.registrerttidspunkt,
    s.ferdigbehandlettidspunkt, s.vedtaktidspunkt, s.utbetalttidspunkt,
    s.endrettidspunkt, s.soknadsformat, s.forventetoppstarttidspunkt,
    now() AS teknisktidspunkt,
    s.sakytelse, s.behandlingtype, s.behandlingstatus, s.behandlingresultat,
    s.resultatbegrunnelse, s.behandlingmetode, s.opprettetav, s.saksbehandler,
    s.ansvarligbeslutter, s.tilbakekrevingsbelop,
    s.funksjonellperiode_fra_og_med, s.funksjonellperiode_til_og_med,
    s.hendelse, s.avsender, s.versjon, s.behandling_aarsak,
    s.relatertfagsystem, s.sakutland, s.ansvarligenhet
FROM statistikk_sak s
JOIN klagebehandling kb ON s.behandlingid = kb.id::varchar
WHERE s.behandlingtype = 'KLAGE';