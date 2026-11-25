-- Tiltakspengesaker og vedtak i Arena per måned, hentet daglig fra tiltakspenger-arena.
-- Tallene tas vare på fordi Arena skal stenges.
CREATE TABLE IF NOT EXISTS arena_statistikk_per_maned
(
    maned           date PRIMARY KEY,
    saker_med_rett  integer     NOT NULL,
    nye_rettigheter integer     NOT NULL,
    gjenopptak      integer     NOT NULL,
    endringer       integer     NOT NULL,
    stans           integer     NOT NULL,
    hentet          timestamptz NOT NULL
);
