CREATE TABLE utbetalingsoversikt
(
    id                  varchar     primary key,
    sak_id              varchar     not null references sak (id),
    hentet              timestamptz not null,
    oppslag_fom         date        not null,
    oppslag_tom         date        not null,
    oppslag_periodetype varchar     not null check (oppslag_periodetype IN ('UTBETALINGSPERIODE', 'YTELSESPERIODE')),
    resultat            varchar     not null check (resultat IN ('VELLYKKET', 'FEILET')),
    feiltype            varchar     null check (feiltype IN ('TILGANG_AVVIST', 'TJENESTEFEIL', 'SAK_AVVIST', 'ULESELIG_SVAR', 'UGYLDIG_INNHOLD')),
    utbetalinger        jsonb       null,
    metadata            jsonb       not null,
    neste_oppslag       timestamptz not null,
    antall_feil_på_rad  integer     not null check (antall_feil_på_rad >= 0),

    CONSTRAINT utbetalingsoversikt_oppslagsperiode CHECK (oppslag_fom <= oppslag_tom),
    CONSTRAINT utbetalingsoversikt_neste_oppslag CHECK (neste_oppslag >= hentet),
    CONSTRAINT utbetalingsoversikt_resultat_innhold CHECK (
        (resultat = 'VELLYKKET' AND utbetalinger IS NOT NULL AND feiltype IS NULL AND antall_feil_på_rad = 0) OR
        (resultat = 'FEILET' AND utbetalinger IS NULL AND feiltype IS NOT NULL AND antall_feil_på_rad > 0)
    )
);

CREATE INDEX utbetalingsoversikt_sak_hentet_idx
    ON utbetalingsoversikt (sak_id, hentet DESC, id DESC);
