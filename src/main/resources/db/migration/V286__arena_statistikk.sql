-- Tiltakspengesaker og personer i Arena med rett etter datoen i kolonnen etter, fordelt på siste dag med rett, hentet daglig fra tiltakspenger-arena.
-- Siste dag med rett lik NULL betyr at retten ikke har sluttdato.
-- Personer telles på sin seneste siste dag med rett på tvers av sakene.
-- Saker og personer med rett etter en dato X >= etter er summen over siste_dag_med_rett > X eller siste_dag_med_rett IS NULL.
-- Tallene tas vare på fordi Arena skal stenges.
CREATE TABLE IF NOT EXISTS arena_siste_dag_med_rett
(
    etter              date        NOT NULL,
    siste_dag_med_rett date        NULL,
    saker              integer     NOT NULL,
    personer           integer     NOT NULL,
    hentet             timestamptz NOT NULL,
    UNIQUE NULLS NOT DISTINCT (siste_dag_med_rett)
);

-- Tabellen har ingen primærnøkkel, og publikasjonen for alle tabeller krever replikaidentitet for å slette rader.
ALTER TABLE arena_siste_dag_med_rett
    REPLICA IDENTITY FULL;

-- Utbetalte tiltakspenger i Arena per måned, hentet daglig fra tiltakspenger-arena.
-- Beløp og dager er fordelt på månedene etter hverdager i meldeperioden.
-- Tallene tas vare på fordi Arena skal stenges.
CREATE TABLE IF NOT EXISTS arena_utbetalinger_per_maned
(
    maned                               date           PRIMARY KEY,
    personer_med_utbetaling             integer        NOT NULL,
    dager_utbetalt                      numeric(12, 2) NOT NULL,
    belop_grunnsats                     numeric(14, 2) NOT NULL,
    belop_barnetillegg                  numeric(14, 2) NOT NULL,
    personer_med_beregnet_ikke_utbetalt integer        NOT NULL,
    belop_beregnet_ikke_utbetalt        numeric(14, 2) NOT NULL,
    hentet                              timestamptz    NOT NULL
);
