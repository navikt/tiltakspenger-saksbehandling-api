-- Utledes nå fra at saksbehandler er den automatiske saksbehandleren (tp-sak).
ALTER TABLE behandling
    DROP COLUMN IF EXISTS automatisk_saksbehandlet;

-- Avslagsgrunnene lagres nå i samme kolonne som hjemlene for stans og opphør.
-- Verdiene har prefikset AVSLAG_, så de kolliderer ikke med STANS_ og OPPHØR_.
UPDATE behandling
SET valgt_hjemmel_har_ikke_rettighet = avslagsgrunner
WHERE resultat = 'AVSLAG';

ALTER TABLE behandling
    DROP COLUMN IF EXISTS avslagsgrunner;

-- Ingen av disse brukes lenger.
ALTER TABLE behandling
    DROP COLUMN IF EXISTS oppgave_id,
    DROP COLUMN IF EXISTS omgjør_rammevedtak_id;
