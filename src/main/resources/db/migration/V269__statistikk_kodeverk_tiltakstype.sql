-- Kodeverk for Team Spenn: hvilken stønadstype vi sender til helved for hver ekstern tiltakskode som gir rett.
-- Tabellen skrives av StatistikkService.synkroniserTiltakstypekodeverk fra koden; se docs/statistikk/README.md.
CREATE TABLE statistikk_kodeverk_tiltakstype
(
    id                  bigserial primary key,
    ekstern_kildesystem varchar                  not null,
    ekstern_tiltakskode varchar                  not null,
    ekstern_stonadstype varchar                  not null,
    gyldig_fra          timestamp with time zone,
    gyldig_til          timestamp with time zone,
    opprettet           timestamp with time zone not null,
    sist_endret         timestamp with time zone not null
);

CREATE UNIQUE INDEX statistikk_kodeverk_tiltakstype_gjeldende
    ON statistikk_kodeverk_tiltakstype (ekstern_kildesystem, ekstern_tiltakskode)
    WHERE gyldig_til IS NULL;

COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.ekstern_kildesystem IS 'Systemet vi mottar tiltakskoden fra: Arena, Komet eller TeamTiltak.';
COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.ekstern_tiltakskode IS 'Innkommende tiltakskode fra kildesystemet. Samme kode kan finnes hos flere kildesystemer.';
COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.ekstern_stonadstype IS 'Utgående stønadstype vi sender til helved for tiltakskoden.';
COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.gyldig_fra IS 'Når mappingen begynte å gjelde hos oss. Null betyr at den gjaldt før kodeverket ble opprettet.';
COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.gyldig_til IS 'Når mappingen sluttet å gjelde hos oss, eksklusivt. Null betyr at den gjelder nå.';
COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.opprettet IS 'Når raden ble lagt inn.';
COMMENT ON COLUMN statistikk_kodeverk_tiltakstype.sist_endret IS 'Når raden sist ble endret. Settes ved innsetting og når gyldig_til settes.';
